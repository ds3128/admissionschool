//package org.darius.notification.kafka.consumers;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.darius.notification.enums.NotificationType;
//import org.darius.notification.events.admission.*;
//import org.darius.notification.kafka.KafkaConfig;
//import org.darius.notification.mail.UserResolverService;
//import org.darius.notification.services.NotificationService;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.support.Acknowledgment;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class AdmissionConsumer {
//
//    private final NotificationService  notificationService;
//    private final UserResolverService  userResolver;
//    private final ObjectMapper         objectMapper;
//
//    // ── application.submitted ─────────────────────────────────────────────────
//    @KafkaListener(
//            topics           = KafkaConfig.TOPIC_APPLICATION_SUBMITTED,
//            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void onApplicationSubmitted(String message, Acknowledgment ack) {
//        log.info("application.submitted reçu");
//        try {
//            var event = objectMapper.readValue(message, ApplicationSubmittedEvent.class);
//
//            notificationService.send(
//                    event.getUserId(),
//                    event.getPersonalEmail(),
//                    NotificationType.APPLICATION_SUBMITTED,
//                    "Dossier reçu - confirmation de candidature",
//                    "mail/admission/application-submitted",
//                    Map.of(
//                            "firstName",     event.getFirstName()     != null ? event.getFirstName()     : "",
//                            "applicationId", event.getApplicationId() != null ? event.getApplicationId() : "",
//                            "academicYear",  event.getAcademicYear()  != null ? event.getAcademicYear()  : "",
//                            "submittedAt",   event.getSubmittedAt()   != null ? event.getSubmittedAt()   : ""
//                    ),
//                    event.getApplicationId(), "APPLICATION"
//            );
//            ack.acknowledge();
//        } catch (Exception ex) {
//            log.error("Erreur application.submitted : {}", ex.getMessage(), ex);
//        }
//    }
//
//    // ── application.admin.review ──────────────────────────────────────────────
//    @KafkaListener(
//            topics           = KafkaConfig.TOPIC_APPLICATION_ADMIN_REVIEW,
//            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void onApplicationAdminReview(String message, Acknowledgment ack) {
//        log.info("application.admin.review reçu");
//        try {
//            var event = objectMapper.readValue(message, ApplicationAdminReviewEvent.class);
//
//            notificationService.send(
//                    event.getUserId(),
//                    event.getPersonalEmail(),
//                    NotificationType.APPLICATION_ADMIN_REVIEW,
//                    "Mise à jour de votre dossier de candidature",
//                    "mail/admission/application-under-review",
//                    Map.of(
//                            "firstName", event.getFirstName(),
//                            "status",    event.getStatus(),
//                            "comment",   event.getComment() != null ? event.getComment() : ""
//                    ),
//                    event.getApplicationId(), "APPLICATION"
//            );
//            ack.acknowledge();
//        } catch (Exception ex) {
//            log.error("Erreur application.admin.review : {}", ex.getMessage(), ex);
//        }
//    }
//
//    // ── application.pending.commission ────────────────────────────────────────
//    @KafkaListener(
//            topics           = KafkaConfig.TOPIC_APPLICATION_PENDING_COMMISSION,
//            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void onApplicationPendingCommission(String message, Acknowledgment ack) {
//        log.info("application.pending.commission reçu");
//        try {
//            var event = objectMapper.readValue(message, ApplicationPendingCommissionEvent.class);
//
//            notificationService.send(
//                    event.getUserId(),
//                    event.getPersonalEmail(),
//                    NotificationType.APPLICATION_PENDING_COMMISSION,
//                    "Votre dossier a été transmis à la commission",
//                    "mail/admission/application-pending-commission",
//                    Map.of(
//                            "firstName",    event.getFirstName(),
//                            "academicYear", event.getAcademicYear()
//                    ),
//                    event.getApplicationId(), "APPLICATION"
//            );
//            ack.acknowledge();
//        } catch (Exception ex) {
//            log.error("Erreur application.pending.commission : {}", ex.getMessage(), ex);
//        }
//    }
//
//    // ── interview.scheduled ───────────────────────────────────────────────────
//    @KafkaListener(
//            topics           = KafkaConfig.TOPIC_INTERVIEW_SCHEDULED,
//            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void onInterviewScheduled(String message, Acknowledgment ack) {
//        log.info("interview.scheduled reçu");
//        try {
//            var event = objectMapper.readValue(message, InterviewScheduledEvent.class);
//
//            notificationService.send(
//                    event.getUserId(),
//                    event.getPersonalEmail(),
//                    NotificationType.INTERVIEW_SCHEDULED,
//                    "Entretien planifié — informations importantes",
//                    "mail/admission/interview-scheduled",
//                    Map.of(
//                            "firstName",   event.getFirstName(),
//                            "scheduledAt", event.getScheduledAt(),
//                            "duration",    event.getDuration(),
//                            "location",    event.getLocation(),
//                            "type",        event.getType()
//                    ),
//                    event.getApplicationId(), "APPLICATION"
//            );
//            ack.acknowledge();
//        } catch (Exception ex) {
//            log.error("Erreur interview.scheduled : {}", ex.getMessage(), ex);
//        }
//    }
//
//    // ── thesis.approval.requested ─────────────────────────────────────────────
//    @KafkaListener(
//            topics           = KafkaConfig.TOPIC_THESIS_APPROVAL_REQUESTED,
//            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void onThesisApprovalRequested(String message, Acknowledgment ack) {
//        log.info("thesis.approval.requested reçu");
//        try {
//            var event = objectMapper.readValue(message, ThesisApprovalRequestedEvent.class);
//
//            // Résolution email du directeur via User Service
//            String directorEmail = userResolver.resolveEmail(event.getDirectorId());
//            if (directorEmail == null) {
//                log.warn("Email directeur introuvable pour directorId={}", event.getDirectorId());
//                ack.acknowledge();
//                return;
//            }
//
//            notificationService.send(
//                    event.getDirectorId(),
//                    directorEmail,
//                    NotificationType.THESIS_APPROVAL_REQUESTED,
//                    "Demande d'encadrement de thèse",
//                    "mail/admission/thesis-approval-requested",
//                    Map.of(
//                            "studentName",     event.getStudentName(),
//                            "researchProject", event.getResearchProject(),
//                            "expiresAt",       event.getExpiresAt()
//                    ),
//                    event.getApplicationId(), "APPLICATION"
//            );
//            ack.acknowledge();
//        } catch (Exception ex) {
//            log.error("Erreur thesis.approval.requested : {}", ex.getMessage(), ex);
//        }
//    }
//
//    // ── application.awaiting.confirmation ────────────────────────────────────
//    @KafkaListener(
//            topics           = KafkaConfig.TOPIC_APPLICATION_AWAITING_CONFIRMATION,
//            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void onApplicationAwaitingConfirmation(String message, Acknowledgment ack) {
//        log.info("application.awaiting.confirmation reçu");
//        try {
//            var event = objectMapper.readValue(message, ApplicationAwaitingConfirmationEvent.class);
//
//            notificationService.send(
//                    event.getUserId(),
//                    event.getPersonalEmail(),
//                    NotificationType.APPLICATION_AWAITING_CONFIRMATION,
//                    "Félicitations — Confirmez votre inscription",
//                    "mail/admission/application-awaiting-confirmation",
//                    Map.of(
//                            "firstName",       event.getFirstName(),
//                            "acceptedChoices", event.getAcceptedChoices(),
//                            "expiresAt",       event.getExpiresAt()
//                    ),
//                    event.getApplicationId(), "APPLICATION"
//            );
//            ack.acknowledge();
//        } catch (Exception ex) {
//            log.error("Erreur application.awaiting.confirmation : {}", ex.getMessage(), ex);
//        }
//    }
//
//    // ── application.accepted ──────────────────────────────────────────────────
//    @KafkaListener(
//            topics           = KafkaConfig.TOPIC_APPLICATION_ACCEPTED,
//            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void onApplicationAccepted(String message, Acknowledgment ack) {
//        log.info("application.accepted reçu");
//        try {
//            var event = objectMapper.readValue(message, ApplicationAcceptedEvent.class);
//
//            // CRITIQUE — toujours envoyé
//            notificationService.sendCritical(
//                    event.getUserId(),
//                    event.getPersonalEmail(),
//                    NotificationType.APPLICATION_ACCEPTED,
//                    "Bienvenue à l'université ! Votre inscription est confirmée",
//                    "mail/admission/application-accepted",
//                    Map.of(
//                            "firstName",     event.getFirstName(),
//                            "lastName",      event.getLastName(),
//                            "studentNumber", event.getStudentNumber(),
//                            "filiereName",   event.getFiliereName() != null ? event.getFiliereName() : "",
//                            "academicYear",  event.getAcademicYear()
//                    ),
//                    event.getApplicationId(), "APPLICATION"
//            );
//            ack.acknowledge();
//        } catch (Exception ex) {
//            log.error("Erreur application.accepted : {}", ex.getMessage(), ex);
//        }
//    }
//
//    // ── application.rejected ──────────────────────────────────────────────────
//    @KafkaListener(
//            topics           = KafkaConfig.TOPIC_APPLICATION_REJECTED,
//            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void onApplicationRejected(String message, Acknowledgment ack) {
//        log.info("application.rejected reçu");
//        try {
//            var event = objectMapper.readValue(message, ApplicationRejectedEvent.class);
//
//            notificationService.send(
//                    event.getUserId(),
//                    event.getPersonalEmail(),
//                    NotificationType.APPLICATION_REJECTED,
//                    "Résultat de votre candidature",
//                    "mail/admission/application-rejected",
//                    Map.of(
//                            "firstName",    event.getFirstName(),
//                            "academicYear", event.getAcademicYear()
//                    ),
//                    event.getApplicationId(), "APPLICATION"
//            );
//            ack.acknowledge();
//        } catch (Exception ex) {
//            log.error("Erreur application.rejected : {}", ex.getMessage(), ex);
//        }
//    }
//
//    // ── waitlist.promoted ─────────────────────────────────────────────────────
//    @KafkaListener(
//            topics           = KafkaConfig.TOPIC_WAITLIST_PROMOTED,
//            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void onWaitlistPromoted(String message, Acknowledgment ack) {
//        log.info("waitlist.promoted reçu");
//        try {
//            var event = objectMapper.readValue(message, WaitlistPromotedEvent.class);
//
//            notificationService.send(
//                    event.getUserId(),
//                    event.getPersonalEmail(),
//                    NotificationType.WAITLIST_PROMOTED,
//                    "Bonne nouvelle — Une place est disponible pour vous !",
//                    "mail/admission/waitlist-promoted",
//                    Map.of(
//                            "firstName",   event.getFirstName(),
//                            "filiereName", event.getFiliereName(),
//                            "expiresAt",   event.getExpiresAt()
//                    ),
//                    event.getApplicationId(), "APPLICATION"
//            );
//            ack.acknowledge();
//        } catch (Exception ex) {
//            log.error("Erreur waitlist.promoted : {}", ex.getMessage(), ex);
//        }
//    }
//
//    // ── choice.auto.confirmed ─────────────────────────────────────────────────
//    @KafkaListener(
//            topics           = KafkaConfig.TOPIC_CHOICE_AUTO_CONFIRMED,
//            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void onChoiceAutoConfirmed(String message, Acknowledgment ack) {
//        log.info("choice.auto.confirmed reçu");
//        try {
//            var event = objectMapper.readValue(message, ChoiceAutoConfirmedEvent.class);
//
//            notificationService.send(
//                    event.getUserId(),
//                    event.getPersonalEmail(),
//                    NotificationType.CHOICE_AUTO_CONFIRMED,
//                    "Confirmation automatique de votre inscription",
//                    "mail/admission/choice-auto-confirmed",
//                    Map.of(
//                            "firstName",   event.getFirstName(),
//                            "filiereName", event.getFiliereName()
//                    ),
//                    event.getApplicationId(), "APPLICATION"
//            );
//            ack.acknowledge();
//        } catch (Exception ex) {
//            log.error("Erreur choice.auto.confirmed : {}", ex.getMessage(), ex);
//        }
//    }
//}

package org.darius.notification.kafka.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.notification.enums.NotificationType;
import org.darius.notification.events.admission.*;
import org.darius.notification.kafka.KafkaConfig;
import org.darius.notification.mail.UserResolverService;
import org.darius.notification.services.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdmissionConsumer {

    private final NotificationService  notificationService;
    private final UserResolverService  userResolver;
    private final ObjectMapper         objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── application.submitted ─────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_APPLICATION_SUBMITTED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onApplicationSubmitted(String message, Acknowledgment ack) {
        log.info("application.submitted reçu");
        try {
            var event = objectMapper.readValue(message, ApplicationSubmittedEvent.class);

            String email = resolveEmail(event.getPersonalEmail(), event.getUserId());
            if (email == null) {
                ack.acknowledge();
                return;
            }

            UserResolverService.UserInfo resolve = userResolver.resolve(event.getUserId());
            String lastName = resolveFirstName(resolve.lastName(), event.getUserId());

            Map<String, Object> vars = new HashMap<>();
            vars.put("firstName",     lastName);
            vars.put("applicationId", nvl(event.getApplicationId().toUpperCase()));
            vars.put("academicYear",  nvl(event.getAcademicYear()));

            String formattedDate = "";
            if (event.getSubmittedAt() != null) {
                formattedDate = (event.getSubmittedAt()).format(DATE_FORMATTER);
            }

            vars.put("submittedAt",nvl(formattedDate));

            notificationService.send(
                    event.getUserId(), email,
                    NotificationType.APPLICATION_SUBMITTED,
                    "Dossier reçu - confirmation de candidature",
                    "mail/admission/application-submitted",
                    vars,
                    event.getApplicationId(), "APPLICATION"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur application.submitted : {}", ex.getMessage(), ex);
        }
    }

    // ── application.admin.review ──────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_APPLICATION_ADMIN_REVIEW,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onApplicationAdminReview(String message, Acknowledgment ack) {
        log.info("application.admin.review reçu");
        try {
            var event = objectMapper.readValue(message, ApplicationAdminReviewEvent.class);

            String email = resolveEmail(event.getPersonalEmail(), event.getUserId());
            if (email == null) { ack.acknowledge(); return; }

            String firstName = resolveFirstName(event.getFirstName(), event.getUserId());

            Map<String, Object> vars = new HashMap<>();
            vars.put("firstName", firstName);
            vars.put("status",    nvl(event.getStatus()));
            vars.put("comment",   nvl(event.getComment()));

            notificationService.send(
                    event.getUserId(), email,
                    NotificationType.APPLICATION_ADMIN_REVIEW,
                    "Mise à jour de votre dossier de candidature",
                    "mail/admission/application-under-review",
                    vars,
                    event.getApplicationId(), "APPLICATION"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur application.admin.review : {}", ex.getMessage(), ex);
        }
    }

    // ── application.pending.commission ────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_APPLICATION_PENDING_COMMISSION,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onApplicationPendingCommission(String message, Acknowledgment ack) {
        log.info("application.pending.commission reçu");
        try {
            var event = objectMapper.readValue(message, ApplicationPendingCommissionEvent.class);

            String email = resolveEmail(event.getPersonalEmail(), event.getUserId());
            if (email == null) { ack.acknowledge(); return; }

            String firstName = resolveFirstName(event.getFirstName(), event.getUserId());

            Map<String, Object> vars = new HashMap<>();
            vars.put("firstName",    firstName);
            vars.put("academicYear", nvl(event.getAcademicYear()));

            notificationService.send(
                    event.getUserId(), email,
                    NotificationType.APPLICATION_PENDING_COMMISSION,
                    "Votre dossier a été transmis à la commission",
                    "mail/admission/application-pending-commission",
                    vars,
                    event.getApplicationId(), "APPLICATION"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur application.pending.commission : {}", ex.getMessage(), ex);
        }
    }

    // ── interview.scheduled ───────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_INTERVIEW_SCHEDULED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onInterviewScheduled(String message, Acknowledgment ack) {
        log.info("interview.scheduled reçu");
        try {
            var event = objectMapper.readValue(message, InterviewScheduledEvent.class);

            String email = resolveEmail(event.getPersonalEmail(), event.getUserId());
            if (email == null) { ack.acknowledge(); return; }

            String firstName = resolveFirstName(event.getFirstName(), event.getUserId());

            Map<String, Object> vars = new HashMap<>();
            vars.put("firstName",   firstName);
            vars.put("scheduledAt", nvl(event.getScheduledAt()));
            vars.put("duration",    nvl(event.getDuration()));
            vars.put("location",    nvl(event.getLocation()));
            vars.put("type",        nvl(event.getType()));

            notificationService.send(
                    event.getUserId(), email,
                    NotificationType.INTERVIEW_SCHEDULED,
                    "Entretien planifié — informations importantes",
                    "mail/admission/interview-scheduled",
                    vars,
                    event.getApplicationId(), "APPLICATION"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur interview.scheduled : {}", ex.getMessage(), ex);
        }
    }

    // ── thesis.approval.requested ─────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_THESIS_APPROVAL_REQUESTED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onThesisApprovalRequested(String message, Acknowledgment ack) {
        log.info("thesis.approval.requested reçu");
        try {
            var event = objectMapper.readValue(message, ThesisApprovalRequestedEvent.class);

            String directorEmail = userResolver.resolveEmail(event.getDirectorId());
            if (directorEmail == null) {
                log.warn("Email directeur introuvable pour directorId={}", event.getDirectorId());
                ack.acknowledge();
                return;
            }

            Map<String, Object> vars = new HashMap<>();
            vars.put("studentName",     nvl(event.getStudentName()));
            vars.put("researchProject", nvl(event.getResearchProject()));
            vars.put("expiresAt",       nvl(event.getExpiresAt()));

            notificationService.send(
                    event.getDirectorId(), directorEmail,
                    NotificationType.THESIS_APPROVAL_REQUESTED,
                    "Demande d'encadrement de thèse",
                    "mail/admission/thesis-approval-requested",
                    vars,
                    event.getApplicationId(), "APPLICATION"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur thesis.approval.requested : {}", ex.getMessage(), ex);
        }
    }

    // ── application.awaiting.confirmation ────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_APPLICATION_AWAITING_CONFIRMATION,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onApplicationAwaitingConfirmation(String message, Acknowledgment ack) {
        log.info("application.awaiting.confirmation reçu");
        try {
            var event = objectMapper.readValue(message, ApplicationAwaitingConfirmationEvent.class);

            String email = resolveEmail(event.getPersonalEmail(), event.getUserId());
            if (email == null) { ack.acknowledge(); return; }

            String firstName = resolveFirstName(event.getFirstName(), event.getUserId());

            Map<String, Object> vars = new HashMap<>();
            vars.put("firstName",       firstName);
            vars.put("acceptedChoices", event.getAcceptedChoices() != null ? event.getAcceptedChoices() : java.util.List.of());
            vars.put("expiresAt",       nvl(event.getExpiresAt()));

            notificationService.send(
                    event.getUserId(), email,
                    NotificationType.APPLICATION_AWAITING_CONFIRMATION,
                    "Félicitations — Confirmez votre inscription",
                    "mail/admission/application-awaiting-confirmation",
                    vars,
                    event.getApplicationId(), "APPLICATION"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur application.awaiting.confirmation : {}", ex.getMessage(), ex);
        }
    }

    // ── application.accepted ──────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_APPLICATION_ACCEPTED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onApplicationAccepted(String message, Acknowledgment ack) {
        log.info("application.accepted reçu");
        try {
            var event = objectMapper.readValue(message, ApplicationAcceptedEvent.class);

            String email = resolveEmail(event.getPersonalEmail(), event.getUserId());
            if (email == null) { ack.acknowledge(); return; }

            String firstName = resolveFirstName(event.getFirstName(), event.getUserId());
            UserResolverService.UserInfo info = userResolver.resolve(event.getUserId());
            String lastName = (event.getLastName() != null && !event.getLastName().isBlank())
                    ? event.getLastName()
                    : (info != null ? nvl(info.lastName()) : "");

            Map<String, Object> vars = new HashMap<>();
            vars.put("firstName",     firstName);
            vars.put("lastName",      lastName);
            vars.put("studentNumber", nvl(event.getStudentNumber()));
            vars.put("filiereName",   nvl(event.getFiliereName()));
            vars.put("academicYear",  nvl(event.getAcademicYear()));

            notificationService.sendCritical(
                    event.getUserId(), email,
                    NotificationType.APPLICATION_ACCEPTED,
                    "Bienvenue à l'université ! Votre inscription est confirmée",
                    "mail/admission/application-accepted",
                    vars,
                    event.getApplicationId(), "APPLICATION"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur application.accepted : {}", ex.getMessage(), ex);
        }
    }

    // ── application.rejected ──────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_APPLICATION_REJECTED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onApplicationRejected(String message, Acknowledgment ack) {
        log.info("application.rejected reçu");
        try {
            var event = objectMapper.readValue(message, ApplicationRejectedEvent.class);

            String email = resolveEmail(event.getPersonalEmail(), event.getUserId());
            if (email == null) { ack.acknowledge(); return; }

            String firstName = resolveFirstName(event.getFirstName(), event.getUserId());

            Map<String, Object> vars = new HashMap<>();
            vars.put("firstName",    firstName);
            vars.put("academicYear", nvl(event.getAcademicYear()));

            notificationService.send(
                    event.getUserId(), email,
                    NotificationType.APPLICATION_REJECTED,
                    "Résultat de votre candidature",
                    "mail/admission/application-rejected",
                    vars,
                    event.getApplicationId(), "APPLICATION"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur application.rejected : {}", ex.getMessage(), ex);
        }
    }

    // ── waitlist.promoted ─────────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_WAITLIST_PROMOTED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onWaitlistPromoted(String message, Acknowledgment ack) {
        log.info("waitlist.promoted reçu");
        try {
            var event = objectMapper.readValue(message, WaitlistPromotedEvent.class);

            String email = resolveEmail(event.getPersonalEmail(), event.getUserId());
            if (email == null) { ack.acknowledge(); return; }

            String firstName = resolveFirstName(event.getFirstName(), event.getUserId());

            Map<String, Object> vars = new HashMap<>();
            vars.put("firstName",   firstName);
            vars.put("filiereName", nvl(event.getFiliereName()));
            vars.put("expiresAt",   nvl(event.getExpiresAt()));

            notificationService.send(
                    event.getUserId(), email,
                    NotificationType.WAITLIST_PROMOTED,
                    "Bonne nouvelle — Une place est disponible pour vous !",
                    "mail/admission/waitlist-promoted",
                    vars,
                    event.getApplicationId(), "APPLICATION"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur waitlist.promoted : {}", ex.getMessage(), ex);
        }
    }

    // ── choice.auto.confirmed ─────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_CHOICE_AUTO_CONFIRMED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onChoiceAutoConfirmed(String message, Acknowledgment ack) {
        log.info("choice.auto.confirmed reçu");
        try {
            var event = objectMapper.readValue(message, ChoiceAutoConfirmedEvent.class);

            String email = resolveEmail(event.getPersonalEmail(), event.getUserId());
            if (email == null) { ack.acknowledge(); return; }

            String firstName = resolveFirstName(event.getFirstName(), event.getUserId());

            Map<String, Object> vars = new HashMap<>();
            vars.put("firstName",   firstName);
            vars.put("filiereName", nvl(event.getFiliereName()));

            notificationService.send(
                    event.getUserId(), email,
                    NotificationType.CHOICE_AUTO_CONFIRMED,
                    "Confirmation automatique de votre inscription",
                    "mail/admission/choice-auto-confirmed",
                    vars,
                    event.getApplicationId(), "APPLICATION"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur choice.auto.confirmed : {}", ex.getMessage(), ex);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveEmail(String emailFromEvent, String userId) {
        if (emailFromEvent != null && !emailFromEvent.isBlank()) return emailFromEvent;
        String resolved = userResolver.resolveEmail(userId);
        if (resolved == null) log.warn("Email introuvable pour userId={}", userId);
        return resolved;
    }

    private String resolveFirstName(String firstNameFromEvent, String userId) {
        if (firstNameFromEvent != null && !firstNameFromEvent.isBlank()) return firstNameFromEvent;
        String resolved = userResolver.resolveFirstName(userId);
        return resolved != null ? resolved : "";
    }

    private String nvl(Object value) {
        return value != null ? value.toString() : "";
    }
}

