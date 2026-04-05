package org.darius.admission.services.impls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.admission.common.enums.ApplicationStatus;
import org.darius.admission.common.enums.ChoiceStatus;
import org.darius.admission.common.enums.ConfirmationStatus;
import org.darius.admission.entities.*;
import org.darius.admission.evens.published.ApplicationAcceptedEvent;
import org.darius.admission.evens.published.ApplicationAwaitingConfirmationEvent;
import org.darius.admission.evens.published.ApplicationRejectedEvent;
import org.darius.admission.kafka.AdmissionEventProducer;
import org.darius.admission.repositories.*;
import org.darius.admission.services.ApplicationEvaluationService;
import org.darius.admission.services.StudentNumberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationEvaluationServiceImpl implements ApplicationEvaluationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationChoiceRepository choiceRepository;
    private final ConfirmationRequestRepository confirmationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final CandidateProfileRepository profileRepository;
    private final AdmissionOfferRepository       offerRepository;
    private final StudentNumberService studentNumberService;
    private final AdmissionEventProducer eventProducer;

    @Override
    @Transactional
    public void evaluateApplication(String applicationId) {
        if (!choiceRepository.allChoicesHaveFinalStatus(applicationId)) {
            log.debug("Évaluation reportée : des choix sont encore en cours pour {}", applicationId);
            return;
        }

        Application app = applicationRepository.findById(applicationId).orElse(null);
        if (app == null) return;

        List<ApplicationChoice> acceptedChoices =
                choiceRepository.findByApplication_IdAndStatus(applicationId, ChoiceStatus.ACCEPTED);

        if (!acceptedChoices.isEmpty()) {
            // Au moins 1 ACCEPTED → créer ConfirmationRequest
            LocalDateTime expiresAt = LocalDateTime.now()
                    .plusDays(app.getCampaign().getConfirmationDeadlineDays());

            ConfirmationRequest confirmation = ConfirmationRequest.builder()
                    .application(app)
                    .acceptedChoiceIds(acceptedChoices.stream().map(ApplicationChoice::getId).toList())
                    .expiresAt(expiresAt)
                    .status(ConfirmationStatus.PENDING)
                    .build();

            confirmationRepository.save(confirmation);

            changeStatus(app, ApplicationStatus.AWAITING_CONFIRMATION,
                    "system", "Au moins un choix accepté");
            applicationRepository.save(app);

            // Notifier le candidat
            CandidateProfile profile = profileRepository
                    .findByApplication_Id(applicationId).orElse(null);

            eventProducer.publishApplicationAwaitingConfirmation(
                    ApplicationAwaitingConfirmationEvent.builder()
                            .applicationId(applicationId)
                            .userId(app.getUserId())
                            .personalEmail(profile != null ? profile.getPersonalEmail() : null)
                            .candidateFirstName(profile != null ? profile.getFirstName() : null)
                            .candidateLastName(profile != null ? profile.getLastName() : null)
                            .acceptedChoices(acceptedChoices.stream()
                                    .map(c -> ApplicationAwaitingConfirmationEvent.AcceptedChoiceSummary.builder()
                                            .choiceId(c.getId())
                                            .filiereName(c.getFiliereName())
                                            .level(c.getLevel().name())
                                            .choiceOrder(c.getChoiceOrder())
                                            .build())
                                    .toList())
                            .expiresAt(expiresAt)
                            .build()
            );

            log.info("Application {} → AWAITING_CONFIRMATION ({} choix acceptés)",
                    applicationId, acceptedChoices.size());

        } else {
            // Tous REJECTED ou WAITLISTED
            List<ApplicationChoice> waitlistedChoices =
                    choiceRepository.findByApplication_IdAndStatus(
                            applicationId, ChoiceStatus.WAITLISTED
                    );

            if (waitlistedChoices.isEmpty()) {
                // Tous REJECTED
                changeStatus(app, ApplicationStatus.REJECTED, "system", "Tous les choix refusés");
                applicationRepository.save(app);

                CandidateProfile profile = profileRepository
                        .findByApplication_Id(applicationId).orElse(null);

                eventProducer.publishApplicationRejected(
                        ApplicationRejectedEvent.builder()
                                .applicationId(applicationId)
                                .userId(app.getUserId())
                                .personalEmail(profile != null ? profile.getPersonalEmail() : null)
                                .candidateFirstName(profile != null ? profile.getFirstName() : null)
                                .candidateLastName(profile != null ? profile.getLastName() : null)
                                .academicYear(app.getAcademicYear())
                                .build()
                );

                log.info("Application {} → REJECTED", applicationId);
            }
            // Si WAITLISTED → on attend la promotion
        }
    }

    @Override
    @Transactional
    public void processExpiredConfirmations() {
        applicationRepository.findExpiredConfirmations().forEach(app -> {
            // Confirmer automatiquement le choix ACCEPTED avec le plus petit choiceOrder
            choiceRepository.findFirstByApplication_IdAndStatusOrderByChoiceOrder(
                    app.getId(), ChoiceStatus.ACCEPTED
            ).ifPresent(choice -> {
                choice.setStatus(ChoiceStatus.CONFIRMED);
                choice.setDecidedAt(LocalDateTime.now());

                // Retirer les autres choix ACCEPTED
                choiceRepository.findByApplication_IdAndStatus(app.getId(), ChoiceStatus.ACCEPTED)
                        .stream()
                        .filter(c -> !c.getId().equals(choice.getId()))
                        .forEach(c -> {
                            c.setStatus(ChoiceStatus.WITHDRAWN);
                            c.getOffer().setCurrentCount(
                                    Math.max(0, c.getOffer().getCurrentCount() - 1)
                            );
                        });

                // Incrémenter acceptedCount
                choice.getOffer().setAcceptedCount(choice.getOffer().getAcceptedCount() + 1);
                offerRepository.save(choice.getOffer());

                // Mettre à jour la ConfirmationRequest
                confirmationRepository.findByApplication_Id(app.getId()).ifPresent(cr -> {
                    cr.setStatus(ConfirmationStatus.EXPIRED);
                    cr.setConfirmedChoiceId(choice.getId());
                    cr.setAutoConfirmed(true);
                    cr.setConfirmedAt(LocalDateTime.now());
                    confirmationRepository.save(cr);
                });

                // Changer le statut
                changeStatus(app, ApplicationStatus.ACCEPTED,
                        "system", "Confirmation automatique - délai expiré");
                applicationRepository.save(app);

                // Générer le matricule et publier ApplicationAccepted
                String studentNumber = studentNumberService.generateStudentNumber();
                CandidateProfile profile = profileRepository
                        .findByApplication_Id(app.getId()).orElse(null);

                eventProducer.publishApplicationAccepted(
                        ApplicationAcceptedEvent.builder()
                                .applicationId(app.getId())
                                .userId(app.getUserId())
                                .studentNumber(studentNumber)
                                .filiereId(choice.getFiliereId())
                                .personalEmail(profile != null ? profile.getPersonalEmail() : null)
                                .firstName(profile != null ? profile.getFirstName() : null)
                                .lastName(profile != null ? profile.getLastName() : null)
                                .birthDate(profile != null ? profile.getBirthDate() : null)
                                .birthPlace(profile != null ? profile.getBirthPlace() : null)
                                .nationality(profile != null ? profile.getNationality() : null)
                                .gender(profile != null ? profile.getGender() : null)
                                .phone(profile != null ? profile.getPhone() : null)
                                .photoUrl(profile != null ? profile.getPhotoUrl() : null)
                                .graduationYear(profile != null && profile.getGraduationYear() != null
                                        ? profile.getGraduationYear() : 0)
                                .autoConfirmed(true)
                                .academicYear(app.getAcademicYear())
                                .build()
                );

                log.info("Confirmation automatique : applicationId={}, studentNumber={}",
                        app.getId(), studentNumber);
            });
        });
    }

    private void changeStatus(
            Application app,
            ApplicationStatus newStatus,
            String changedBy,
            String comment
    ) {
        ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                .application(app)
                .fromStatus(app.getStatus())
                .toStatus(newStatus)
                .changedBy(changedBy)
                .comment(comment)
                .build();
        app.getStatusHistory().add(history);
        app.setStatus(newStatus);
        app.setLastStatusChange(LocalDateTime.now());
    }
}