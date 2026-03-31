package org.darius.admission.services.impls;

import lombok.extern.slf4j.Slf4j;
import org.darius.admission.common.dtos.requests.AdminReviewRequest;
import org.darius.admission.common.dtos.requests.RequestAdditionalDocsRequest;
import org.darius.admission.common.dtos.responses.AdminStatsResponse;
import org.darius.admission.common.dtos.responses.ApplicationResponse;
import org.darius.admission.common.dtos.responses.ApplicationSummaryResponse;
import org.darius.admission.common.dtos.responses.PageResponse;
import org.darius.admission.common.enums.ApplicationStatus;
import org.darius.admission.common.enums.ChoiceStatus;
import org.darius.admission.entities.Application;
import org.darius.admission.entities.ApplicationStatusHistory;
import org.darius.admission.entities.CandidateProfile;
import org.darius.admission.evens.published.ApplicationAdminReviewEvent;
import org.darius.admission.evens.published.ApplicationPendingCommissionEvent;
import org.darius.admission.exceptions.InvalidOperationException;
import org.darius.admission.exceptions.ResourceNotFoundException;
import org.darius.admission.kafka.AdmissionEventProducer;
import org.darius.admission.mappers.AdmissionMapper;
import org.darius.admission.repositories.*;
import org.darius.admission.services.AdminApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminApplicationServiceImpl implements AdminApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationChoiceRepository choiceRepository;
    private final AdmissionOfferRepository offerRepository;
    private final DossierRepository dossierRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final CandidateProfileRepository profileRepository;
    private final AdmissionEventProducer eventProducer;
    private final AdmissionMapper mapper;

    public AdminApplicationServiceImpl(ApplicationRepository applicationRepository, ApplicationChoiceRepository choiceRepository, AdmissionOfferRepository offerRepository, DossierRepository dossierRepository, ApplicationStatusHistoryRepository historyRepository, CandidateProfileRepository profileRepository, AdmissionEventProducer eventProducer, AdmissionMapper mapper) {
        this.applicationRepository = applicationRepository;
        this.choiceRepository = choiceRepository;
        this.offerRepository = offerRepository;
        this.dossierRepository = dossierRepository;
        this.historyRepository = historyRepository;
        this.profileRepository = profileRepository;
        this.eventProducer = eventProducer;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ApplicationSummaryResponse> getAllApplications(
            ApplicationStatus status, Long campaignId, int page, int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        Page<Application> applications = applicationRepository.findWithFilters(
                status, campaignId, pageable
        );

        List<ApplicationSummaryResponse> content = applications.getContent().stream()
                .map(app -> {
                    CandidateProfile profile = profileRepository
                            .findByApplication_Id(app.getId()).orElse(null);
                    return mapper.toApplicationSummaryResponse(app, profile);
                })
                .toList();

        return PageResponse.<ApplicationSummaryResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(applications.getTotalElements())
                .totalPages(applications.getTotalPages())
                .last(applications.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationByIdAdmin(String applicationId) {
        Application app = findOrThrow(applicationId);
        return mapper.toApplicationResponse(app);
    }

    @Override
    @Transactional
    public ApplicationResponse processAdminReview(
            String applicationId, String adminUserId, AdminReviewRequest request
    ) {
        Application app = findOrThrow(applicationId);

        if (app.getStatus() != ApplicationStatus.SUBMITTED
                && app.getStatus() != ApplicationStatus.ADDITIONAL_DOCS_REQUIRED) {
            throw new InvalidOperationException(
                    "La candidature doit être en statut SUBMITTED ou ADDITIONAL_DOCS_REQUIRED"
            );
        }

        // Prise en charge → UNDER_ADMIN_REVIEW
        changeStatus(app, ApplicationStatus.UNDER_ADMIN_REVIEW, adminUserId,
                "Prise en charge administrative");
        app = applicationRepository.save(app);

        CandidateProfile profile = profileRepository
                .findByApplication_Id(applicationId).orElse(null);

        if (request.isApproved()) {
            // Approuvé → PENDING_COMMISSION
            changeStatus(app, ApplicationStatus.PENDING_COMMISSION, adminUserId,
                    request.getComment() != null ? request.getComment() : "Dossier conforme");

            // Passer tous les choix actifs en PENDING_COMMISSION
            Application finalApp = app;
            choiceRepository.findActiveChoices(applicationId).forEach(c -> {
                c.setStatus(ChoiceStatus.PENDING_COMMISSION);
                choiceRepository.save(c);

                // Publier un event pour chaque choix
                eventProducer.publishApplicationPendingCommission(
                        ApplicationPendingCommissionEvent.builder()
                                .applicationId(applicationId)
                                .userId(finalApp.getUserId())
                                .personalEmail(profile != null ? profile.getPersonalEmail() : null)
                                .offerId(c.getOffer().getId())
                                .filiereName(c.getFiliereName())
                                .build()
                );
            });

        } else {
            // Non conforme → ADDITIONAL_DOCS_REQUIRED
            changeStatus(app, ApplicationStatus.ADDITIONAL_DOCS_REQUIRED, adminUserId,
                    request.getComment() != null ? request.getComment() : "Documents non conformes");

            // Déverrouiller le dossier
            dossierRepository.findByApplication_Id(applicationId).ifPresent(d -> {
                d.setLocked(false);
                d.setUnlockReason(request.getComment());
                dossierRepository.save(d);
            });

            // Notifier le candidat
            eventProducer.publishApplicationAdminReview(
                    ApplicationAdminReviewEvent.builder()
                            .applicationId(applicationId)
                            .userId(app.getUserId())
                            .personalEmail(profile != null ? profile.getPersonalEmail() : null)
                            .approved(false)
                            .comment(request.getComment())
                            .build()
            );
        }

        app = applicationRepository.save(app);
        log.info("Révision admin : applicationId={}, approved={}", applicationId, request.isApproved());
        return mapper.toApplicationResponse(app);
    }

    @Override
    @Transactional
    public ApplicationResponse requestAdditionalDocuments(
            String applicationId, String adminUserId, RequestAdditionalDocsRequest request
    ) {
        Application app = findOrThrow(applicationId);

        changeStatus(app, ApplicationStatus.ADDITIONAL_DOCS_REQUIRED, adminUserId, request.getReason());

        dossierRepository.findByApplication_Id(applicationId).ifPresent(d -> {
            d.setLocked(false);
            d.setUnlockReason(request.getReason());
            dossierRepository.save(d);
        });

        app = applicationRepository.save(app);
        return mapper.toApplicationResponse(app);
    }

    @Override
    @Transactional
    public ApplicationResponse forwardToCommission(String applicationId, String adminUserId) {
        Application app = findOrThrow(applicationId);

        if (app.getStatus() != ApplicationStatus.UNDER_ADMIN_REVIEW) {
            throw new InvalidOperationException(
                    "La candidature doit être en cours de révision administrative"
            );
        }

        changeStatus(app, ApplicationStatus.PENDING_COMMISSION, adminUserId,
                "Transmis manuellement à la commission");

        choiceRepository.findActiveChoices(applicationId).forEach(c -> {
            c.setStatus(ChoiceStatus.PENDING_COMMISSION);
            choiceRepository.save(c);
        });

        app = applicationRepository.save(app);
        return mapper.toApplicationResponse(app);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats(Long campaignId) {
        List<Application> apps = (campaignId != null)
                ? applicationRepository.findByCampaign_Id(campaignId)
                : applicationRepository.findAll();

        long total     = apps.size();
        long submitted = apps.stream().filter(a -> a.getStatus() != ApplicationStatus.DRAFT).count();
        long confirmed = apps.stream().filter(a -> a.getStatus() == ApplicationStatus.CONFIRMED).count();

        List<AdminStatsResponse.OfferStatLine> byOffer = offerRepository.findAll().stream()
                .map(offer -> {
                    long conf = choiceRepository.countByOffer_IdAndStatus(
                            offer.getId(), ChoiceStatus.CONFIRMED);
                    long wait = choiceRepository.countByOffer_IdAndStatus(
                            offer.getId(), ChoiceStatus.WAITLISTED);
                    return AdminStatsResponse.OfferStatLine.builder()
                            .offerId(offer.getId())
                            .filiereName(offer.getFiliereName())
                            .level(offer.getLevel().name())
                            .maxCapacity(offer.getMaxCapacity())
                            .confirmed(conf)
                            .waitlisted(wait)
                            .fillRate(offer.getMaxCapacity() > 0
                                    ? (double) conf / offer.getMaxCapacity() * 100 : 0)
                            .build();
                })
                .collect(Collectors.toList());

        return AdminStatsResponse.builder()
                .totalApplications(total)
                .draft(apps.stream().filter(a -> a.getStatus() == ApplicationStatus.DRAFT).count())
                .submitted(submitted)
                .pendingAdminReview(apps.stream()
                        .filter(a -> a.getStatus() == ApplicationStatus.PENDING_ADMIN).count())
                .pendingCommission(apps.stream()
                        .filter(a -> a.getStatus() == ApplicationStatus.PENDING_COMMISSION).count())
                .awaitingConfirmation(apps.stream()
                        .filter(a -> a.getStatus() == ApplicationStatus.AWAITING_CONFIRMATION).count())
                .confirmed(confirmed)
                .rejected(apps.stream()
                        .filter(a -> a.getStatus() == ApplicationStatus.REJECTED).count())
                .expired(apps.stream()
                        .filter(a -> a.getStatus() == ApplicationStatus.CONFIRMATION_EXPIRED).count())
                .acceptanceRate(submitted > 0 ? (double) confirmed / submitted * 100 : 0)
                .byOffer(byOffer)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToCsv(Long campaignId, String status) {
        List<Application> apps = (campaignId != null)
                ? applicationRepository.findByCampaign_Id(campaignId)
                : applicationRepository.findAll();

        if (status != null && !status.isBlank()) {
            ApplicationStatus st = ApplicationStatus.valueOf(status.toUpperCase());
            apps = apps.stream()
                    .filter(a -> a.getStatus() == st)
                    .toList();
        }

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Statut,Campagne,FiliereName,Niveau,DateSoumission\n");

        for (Application app : apps) {
            csv.append(app.getId()).append(",")
                    .append(app.getStatus()).append(",")
                    .append(app.getCampaign().getAcademicYear()).append(",")
                    .append(app.getChoices().isEmpty() ? "" :
                            app.getChoices().getFirst().getOffer().getFiliereName()).append(",")
                    .append(app.getChoices().isEmpty() ? "" :
                            app.getChoices().getFirst().getOffer().getLevel()).append(",")
                    .append(app.getSubmittedAt() != null ? app.getSubmittedAt() : "").append("\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }


    // ── Helpers ───────────────────────────────────────────────────────────────

    private Application findOrThrow(String id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable : id=" + id));
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