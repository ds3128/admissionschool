package org.darius.admission.services.impls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.admission.common.dtos.requests.*;
import org.darius.admission.common.dtos.responses.*;
import org.darius.admission.common.enums.*;
import org.darius.admission.entities.*;
import org.darius.admission.evens.published.ApplicationAcceptedEvent;
import org.darius.admission.evens.published.ApplicationSubmittedEvent;
import org.darius.admission.exceptions.DuplicateResourceException;
import org.darius.admission.exceptions.ForbiddenException;
import org.darius.admission.exceptions.InvalidOperationException;
import org.darius.admission.exceptions.ResourceNotFoundException;
import org.darius.admission.kafka.AdmissionEventProducer;
import org.darius.admission.mappers.AdmissionMapper;
import org.darius.admission.repositories.*;
import org.darius.admission.services.ApplicationEvaluationService;
import org.darius.admission.services.ApplicationService;
import org.darius.admission.services.StudentNumberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private static final long   MAX_DOC_SIZE_BYTES = 5L * 1024 * 1024;
    private static final List<String> ALLOWED_MIME  =
            List.of("application/pdf", "image/jpeg", "image/png");

    private final ApplicationRepository applicationRepository;
    private final AdmissionCampaignRepository campaignRepository;
    private final ApplicationChoiceRepository choiceRepository;
    private final AdmissionOfferRepository offerRepository;
    private final DossierRepository              dossierRepository;
    private final DossierDocumentRepository      documentRepository;
    private final CandidateProfileRepository profileRepository;
    private final AdmissionPaymentRepository     paymentRepository;
    private final ConfirmationRequestRepository  confirmationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final StudentNumberService studentNumberService;
    private final ApplicationEvaluationService evaluationService;
    private final AdmissionEventProducer eventProducer;
    private final AdmissionMapper mapper;

    // ── Création ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ApplicationResponse createApplication(String userId, CreateApplicationRequest request) {
        AdmissionCampaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campagne introuvable : id=" + request.getCampaignId()
                ));

        if (campaign.getStatus() != CampaignStatus.OPEN) {
            throw new InvalidOperationException(
                    "Les candidatures ne sont pas ouvertes pour cette campagne"
            );
        }

        if (applicationRepository.existsByUserIdAndCampaign_Id(userId, campaign.getId())) {
            throw new DuplicateResourceException(
                    "Vous avez déjà une candidature pour cette campagne"
            );
        }

        Application application = Application.builder()
                .userId(userId)
                .campaign(campaign)
                .academicYear(campaign.getAcademicYear())
                .status(ApplicationStatus.DRAFT)
                .build();

        application = applicationRepository.save(application);

        // Créer le Dossier vide
        Dossier dossier = Dossier.builder()
                .application(application)
                .build();
        dossierRepository.save(dossier);

        // Créer le CandidateProfile vide
        CandidateProfile profile = CandidateProfile.builder()
                .application(application)
                .build();
        profileRepository.save(profile);

        log.info("Candidature créée : applicationId={}, userId={}", application.getId(), userId);
        return mapper.toApplicationResponse(application);
    }

    // ── Consultation ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(String userId) {
        return applicationRepository.findByUserId(userId).stream()
                .map(mapper::toApplicationResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(String applicationId, String userId) {
        Application app = findApplicationOrThrow(applicationId);
        checkOwnership(app, userId);
        return mapper.toApplicationResponse(app);
    }

    // ── Profil candidat ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public CandidateProfileResponse updateCandidateProfile(
            String applicationId, String userId, UpdateCandidateProfileRequest request
    ) {
        Application app = findApplicationOrThrow(applicationId);
        checkOwnership(app, userId);

        Dossier dossier = dossierRepository.findByApplication_Id(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));

        if (dossier.isLocked()) {
            throw new InvalidOperationException("Le dossier est verrouillé");
        }

        CandidateProfile profile = profileRepository.findByApplication_Id(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil candidat introuvable"));

        if (request.getCurrentInstitution() != null)
            profile.setCurrentInstitution(request.getCurrentInstitution());
        if (request.getCurrentDiploma() != null)
            profile.setCurrentDiploma(request.getCurrentDiploma());
        if (request.getMention() != null)
            profile.setMention(request.getMention());
        if (request.getGraduationYear() != null)
            profile.setGraduationYear(request.getGraduationYear());
        if (request.getResearchProject() != null)
            profile.setResearchProject(request.getResearchProject());
        if (request.getThesisDirectorName() != null)
            profile.setThesisDirectorName(request.getThesisDirectorName());
        if (request.getMotivationLetter() != null)
            profile.setMotivationLetter(request.getMotivationLetter());

        // Recalcul isComplete
        profile.setComplete(isProfileComplete(profile));
        return mapper.toCandidateProfileResponse(profileRepository.save(profile));
    }

    // ── Choix ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ChoiceResponse addChoice(
            String applicationId, String userId, AddChoiceRequest request
    ) {
        Application app = findApplicationOrThrow(applicationId);
        checkOwnership(app, userId);
        checkDraft(app);

        AdmissionOffer offer = offerRepository.findById(request.getOfferId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Offre introuvable : id=" + request.getOfferId()
                ));

        if (offer.getStatus() != OfferStatus.OPEN) {
            throw new InvalidOperationException("Cette offre n'est plus disponible");
        }

        if (offer.getDeadline().isBefore(LocalDate.now())) {
            throw new InvalidOperationException("La date limite de cette offre est dépassée");
        }

        if (choiceRepository.existsByApplication_IdAndOffer_Id(applicationId, offer.getId())) {
            throw new DuplicateResourceException("Vous avez déjà sélectionné cette formation");
        }

        int maxChoices = app.getCampaign().getMaxChoicesPerApplication();
        if (choiceRepository.countActiveChoices(applicationId) >= maxChoices) {
            throw new InvalidOperationException(
                    "Vous avez atteint le nombre maximum de choix (" + maxChoices + ")"
            );
        }

        ApplicationChoice choice = ApplicationChoice.builder()
                .application(app)
                .offer(offer)
                .filiereId(offer.getFiliereId())
                .filiereName(offer.getFiliereName())
                .level(offer.getLevel())
                .choiceOrder(request.getChoiceOrder())
                .status(ChoiceStatus.PENDING_ADMIN)
                .build();

        choice = choiceRepository.save(choice);

        // Incrémenter currentCount
        offer.setCurrentCount(offer.getCurrentCount() + 1);
        offerRepository.save(offer);

        return mapper.toChoiceResponse(choice);
    }

    @Override
    @Transactional
    public void removeChoice(String applicationId, Long choiceId, String userId) {
        Application app = findApplicationOrThrow(applicationId);
        checkOwnership(app, userId);
        checkDraft(app);

        ApplicationChoice choice = choiceRepository.findById(choiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Choix introuvable : id=" + choiceId));

        // Décrémenter currentCount
        AdmissionOffer offer = choice.getOffer();
        offer.setCurrentCount(Math.max(0, offer.getCurrentCount() - 1));
        offerRepository.save(offer);

        choiceRepository.delete(choice);
    }

    @Override
    @Transactional
    public ApplicationResponse reorderChoices(
            String applicationId, String userId, ReorderChoicesRequest request
    ) {
        Application app = findApplicationOrThrow(applicationId);
        checkOwnership(app, userId);
        checkDraft(app);

        request.getChoices().forEach(co -> {
            choiceRepository.findById(co.getChoiceId()).ifPresent(choice -> {
                choice.setChoiceOrder(co.getNewOrder());
                choiceRepository.save(choice);
            });
        });

        return mapper.toApplicationResponse(findApplicationOrThrow(applicationId));
    }

    // ── Documents ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DocumentResponse uploadDocument(
            String applicationId, String userId,
            MultipartFile file, String documentTypeStr
    ) {
        Application app = findApplicationOrThrow(applicationId);
        checkOwnership(app, userId);

        Dossier dossier = dossierRepository.findByApplication_Id(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));

        if (dossier.isLocked()) {
            throw new InvalidOperationException("Le dossier est verrouillé");
        }

        validateFile(file);

        DocumentType type;
        try {
            type = DocumentType.valueOf(documentTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOperationException("Type de document invalide : " + documentTypeStr);
        }

        // Stocker le fichier — TODO : intégration S3/MinIO
        String fileUrl = "/documents/" + applicationId + "/" + type + "_" + file.getOriginalFilename();

        DossierDocument doc = DossierDocument.builder()
                .dossier(dossier)
                .type(type)
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .status(DocumentStatus.PENDING)
                .build();

        doc = documentRepository.save(doc);

        // Recalcul isComplete sur le dossier
        recalculateDossierCompletion(dossier, app);

        return mapper.toDocumentResponse(doc);
    }

    @Override
    @Transactional
    public void removeDocument(String applicationId, Long documentId, String userId) {
        Application app = findApplicationOrThrow(applicationId);
        checkOwnership(app, userId);

        Dossier dossier = dossierRepository.findByApplication_Id(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));

        if (dossier.isLocked()) {
            throw new InvalidOperationException("Le dossier est verrouillé");
        }

        documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable"));
        documentRepository.deleteById(documentId);

        recalculateDossierCompletion(dossier, app);
    }

    // ── Paiement ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse initiatePayment(String applicationId, String userId) {
        Application app = findApplicationOrThrow(applicationId);
        checkOwnership(app, userId);

        if (app.getStatus() == ApplicationStatus.PAID) {
            throw new InvalidOperationException("Les frais ont déjà été payés");
        }

        if (choiceRepository.countActiveChoices(applicationId) == 0) {
            throw new InvalidOperationException(
                    "Vous devez avoir au moins un choix avant de payer"
            );
        }

        AdmissionPayment payment = AdmissionPayment.builder()
                .application(app)
                .amount(app.getCampaign().getFeeAmount())
                .currency("EUR")
                .paymentReference(applicationId)
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Paiement initié : applicationId={}, amount={}", applicationId, payment.getAmount());
        return mapper.toPaymentResponse(payment);
    }

    // ── Soumission ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ApplicationResponse submitApplication(String applicationId, String userId) {
        Application app = findApplicationOrThrow(applicationId);
        checkOwnership(app, userId);

        // Vérifications
        if (app.getStatus() != ApplicationStatus.PAID) {
            throw new InvalidOperationException(
                    "Les frais de dossier doivent être réglés avant la soumission"
            );
        }

        Dossier dossier = dossierRepository.findByApplication_Id(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));

        if (!dossier.isComplete()) {
            throw new InvalidOperationException(
                    "Le dossier est incomplet — tous les documents obligatoires sont requis"
            );
        }

        // Re-vérifier les deadlines des choix actifs
        List<ApplicationChoice> activeChoices = choiceRepository.findActiveChoices(applicationId);
        activeChoices.stream()
                .filter(c -> c.getOffer().getDeadline().isBefore(LocalDate.now()))
                .forEach(c -> {
                    c.setStatus(ChoiceStatus.WITHDRAWN);
                    choiceRepository.save(c);
                    log.warn("Choix {} retiré : deadline dépassée", c.getId());
                });

        // Vérifier qu'il reste des choix valides
        long validChoices = choiceRepository.countActiveChoices(applicationId);
        if (validChoices == 0) {
            throw new InvalidOperationException(
                    "Tous vos choix ont expiré — impossible de soumettre"
            );
        }

        // Verrouiller le dossier
        dossier.setLocked(true);
        dossier.setLockedAt(LocalDateTime.now());
        dossierRepository.save(dossier);

        // Geler le profil candidat
        CandidateProfile profile = profileRepository.findByApplication_Id(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil candidat introuvable"));
        profile.setFrozen(true);
        profile.setFrozenAt(LocalDateTime.now());
        profileRepository.save(profile);

        // Changer le statut
        changeStatus(app, ApplicationStatus.SUBMITTED, "system", "Dossier soumis");
        app.setSubmittedAt(LocalDateTime.now());
        app = applicationRepository.save(app);

        // Publier ApplicationSubmitted
        eventProducer.publishApplicationSubmitted(
                ApplicationSubmittedEvent.builder()
                        .applicationId(app.getId())
                        .userId(app.getUserId())
                        .academicYear(app.getAcademicYear())
                        .candidateFirstName(profile.getFirstName())
                        .candidateLastName(profile.getLastName())
                        .personalEmail(profile.getPersonalEmail())
                        .choiceCount((int) validChoices)
                        .build()
        );

        log.info("Candidature soumise : applicationId={}", applicationId);
        return mapper.toApplicationResponse(app);
    }

    // ── Confirmation ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ApplicationResponse confirmChoice(
            String applicationId, String userId, ConfirmChoiceRequest request
    ) {
        Application app = findApplicationOrThrow(applicationId);
        checkOwnership(app, userId);

        if (app.getStatus() != ApplicationStatus.AWAITING_CONFIRMATION) {
            throw new InvalidOperationException(
                    "La candidature n'est pas en attente de confirmation"
            );
        }

        ConfirmationRequest confirmation = confirmationRepository
                .findByApplication_IdAndStatus(applicationId, ConfirmationStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune demande de confirmation active"
                ));

        if (confirmation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOperationException("Le délai de confirmation a expiré");
        }

        ApplicationChoice confirmedChoice = choiceRepository.findById(request.getChoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Choix introuvable"));

        if (confirmedChoice.getStatus() != ChoiceStatus.ACCEPTED) {
            throw new InvalidOperationException("Ce choix n'a pas été accepté");
        }

        // Confirmer le choix sélectionné
        confirmedChoice.setStatus(ChoiceStatus.CONFIRMED);
        confirmedChoice.setDecidedAt(LocalDateTime.now());
        choiceRepository.save(confirmedChoice);

        // Retirer les autres choix ACCEPTED
        choiceRepository.findByApplication_IdAndStatus(applicationId, ChoiceStatus.ACCEPTED)
                .stream()
                .filter(c -> !c.getId().equals(request.getChoiceId()))
                .forEach(c -> {
                    c.setStatus(ChoiceStatus.WITHDRAWN);
                    choiceRepository.save(c);
                    // Libérer la place
                    AdmissionOffer offer = c.getOffer();
                    offer.setCurrentCount(Math.max(0, offer.getCurrentCount() - 1));
                    offerRepository.save(offer);
                });

        // Incrémenter acceptedCount de l'offre confirmée
        AdmissionOffer confirmedOffer = confirmedChoice.getOffer();
        confirmedOffer.setAcceptedCount(confirmedOffer.getAcceptedCount() + 1);
        offerRepository.save(confirmedOffer);

        // Mettre à jour la demande de confirmation
        confirmation.setStatus(ConfirmationStatus.CONFIRMED);
        confirmation.setConfirmedChoiceId(request.getChoiceId());
        confirmation.setConfirmedAt(LocalDateTime.now());
        confirmationRepository.save(confirmation);

        // Changer le statut de la candidature
        changeStatus(app, ApplicationStatus.ACCEPTED, userId, "Choix confirmé");
        app = applicationRepository.save(app);

        // Générer le numéro matricule
        String studentNumber = studentNumberService.generateStudentNumber();

        // Récupérer le profil candidat
        CandidateProfile profile = profileRepository.findByApplication_Id(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil candidat introuvable"));

        // Publier ApplicationAccepted
        eventProducer.publishApplicationAccepted(
                ApplicationAcceptedEvent.builder()
                        .applicationId(app.getId())
                        .userId(app.getUserId())
                        .studentNumber(studentNumber)
                        .filiereId(confirmedChoice.getFiliereId())
                        .personalEmail(profile.getPersonalEmail())
                        .firstName(profile.getFirstName())
                        .lastName(profile.getLastName())
                        .birthDate(profile.getBirthDate())
                        .birthPlace(profile.getBirthPlace())
                        .nationality(profile.getNationality())
                        .gender(profile.getGender())
                        .phone(profile.getPhone())
                        .address(profile.getAddress())
                        .photoUrl(profile.getPhotoUrl())
                        .currentInstitution(profile.getCurrentInstitution())
                        .currentDiploma(profile.getCurrentDiploma())
                        .graduationYear(profile.getGraduationYear() != null
                                ? profile.getGraduationYear() : 0)
                        .autoConfirmed(false)
                        .academicYear(app.getAcademicYear())
                        .build()
        );

        log.info("Candidature acceptée : applicationId={}, studentNumber={}",
                applicationId, studentNumber);

        return mapper.toApplicationResponse(app);
    }

    @Override
    @Transactional
    public void withdrawApplication(String applicationId, String userId) {
        Application app = findApplicationOrThrow(applicationId);
        checkOwnership(app, userId);

        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new InvalidOperationException(
                    "Seules les candidatures en brouillon peuvent être retirées"
            );
        }

        // Libérer les places des offres
        choiceRepository.findActiveChoices(applicationId).forEach(c -> {
            AdmissionOffer offer = c.getOffer();
            offer.setCurrentCount(Math.max(0, offer.getCurrentCount() - 1));
            offerRepository.save(offer);
        });

        changeStatus(app, ApplicationStatus.WITHDRAWN, userId, "Retirée par le candidat");
        applicationRepository.save(app);
    }

    @Override
    @Transactional(readOnly = true)
    public ConfirmationResponse getConfirmationStatus(String applicationId, String userId) {
        Application app = findApplicationOrThrow(applicationId);
        checkOwnership(app, userId);

        ConfirmationRequest confirmation = confirmationRepository
                .findByApplication_Id(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune demande de confirmation pour cette candidature"
                ));

        return mapper.toConfirmationResponse(confirmation);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Application findApplicationOrThrow(String id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable : id=" + id));
    }

    private void checkOwnership(Application app, String userId) {
        if (!app.getUserId().equals(userId)) {
            throw new ForbiddenException("Vous n'avez pas accès à cette candidature");
        }
    }

    private void checkDraft(Application app) {
        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new InvalidOperationException(
                    "Cette action n'est possible que sur une candidature en brouillon"
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException("Le fichier est vide");
        }
        if (file.getSize() > MAX_DOC_SIZE_BYTES) {
            throw new InvalidOperationException("Fichier trop volumineux (max 5 Mo)");
        }
        if (!ALLOWED_MIME.contains(file.getContentType())) {
            throw new InvalidOperationException(
                    "Format non supporté. Formats acceptés : PDF, JPEG, PNG"
            );
        }
    }

    private void recalculateDossierCompletion(Dossier dossier, Application app) {
        List<ApplicationChoice> choices = choiceRepository.findActiveChoices(app.getId());
        boolean complete = choices.stream().allMatch(c -> {
            try {
                return documentRepository.allMandatoryDocumentsPresent(
                        dossier.getId(), c.getOffer().getId()
                );
            } catch (Exception e) {
                return false;
            }
        });
        dossier.setComplete(complete);
        dossierRepository.save(dossier);
    }

    private boolean isProfileComplete(CandidateProfile profile) {
        return profile.getFirstName() != null
                && profile.getLastName() != null
                && profile.getCurrentInstitution() != null
                && profile.getCurrentDiploma() != null
                && profile.getGraduationYear() != null;
    }

    private void changeStatus(
            Application app, ApplicationStatus newStatus,
            String changedBy, String comment
    ) {
        ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                .application(app)
                .fromStatus(app.getStatus())
                .toStatus(newStatus)
                .changedBy(changedBy)
                .comment(comment)
                .build();
        historyRepository.save(history);

        app.setStatus(newStatus);
        app.setLastStatusChange(LocalDateTime.now());
    }
}