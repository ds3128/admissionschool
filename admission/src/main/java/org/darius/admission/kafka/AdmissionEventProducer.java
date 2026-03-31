package org.darius.admission.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.admission.evens.published.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdmissionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper                  objectMapper;

    // ── Application ───────────────────────────────────────────────────────────

    public void publishApplicationSubmitted(ApplicationSubmittedEvent event) {
        send(KafkaConfig.TOPIC_APPLICATION_SUBMITTED, event.getApplicationId(), event);
    }

    public void publishApplicationAdminReview(ApplicationAdminReviewEvent event) {
        send(KafkaConfig.TOPIC_APPLICATION_ADMIN_REVIEW, event.getApplicationId(), event);
    }

    public void publishApplicationPendingCommission(ApplicationPendingCommissionEvent event) {
        send(KafkaConfig.TOPIC_APPLICATION_PENDING_COMMISSION, event.getApplicationId(), event);
    }

    public void publishApplicationAwaitingConfirmation(ApplicationAwaitingConfirmationEvent event) {
        send(KafkaConfig.TOPIC_APPLICATION_AWAITING_CONFIRMATION, event.getApplicationId(), event);
    }

    public void publishApplicationAccepted(ApplicationAcceptedEvent event) {
        send(KafkaConfig.TOPIC_APPLICATION_ACCEPTED, event.getApplicationId(), event);
    }

    public void publishApplicationRejected(ApplicationRejectedEvent event) {
        send(KafkaConfig.TOPIC_APPLICATION_REJECTED, event.getApplicationId(), event);
    }

    // ── Interview ─────────────────────────────────────────────────────────────

    public void publishInterviewScheduled(InterviewScheduledEvent event) {
        send(KafkaConfig.TOPIC_INTERVIEW_SCHEDULED, event.getApplicationId(), event);
    }

    // ── Thèse ─────────────────────────────────────────────────────────────────

    public void publishThesisApprovalRequested(ThesisDirectorApprovalRequestedEvent event) {
        send(KafkaConfig.TOPIC_THESIS_APPROVAL_REQUESTED, event.getApplicationId(), event);
    }

    // ── Liste d'attente ───────────────────────────────────────────────────────

    public void publishWaitlistPromoted(WaitlistPromotedEvent event) {
        send(KafkaConfig.TOPIC_WAITLIST_PROMOTED, event.getApplicationId(), event);
    }

    public void publishChoiceAutoConfirmed(ChoiceAutoConfirmedEvent event) {
        send(KafkaConfig.TOPIC_CHOICE_AUTO_CONFIRMED, event.getApplicationId(), event);
    }

    public void publishApplicationExpired(ApplicationExpiredEvent event) {
        send(KafkaConfig.TOPIC_APPLICATION_EXPIRED, event.getApplicationId(), event);
    }


    // ── Helper ────────────────────────────────────────────────────────────────

    private void send(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Échec envoi Kafka — topic={}, key={} : {}",
                                    topic, key, ex.getMessage());
                        } else {
                            log.debug("Event publié — topic={}, key={}", topic, key);
                        }
                    });
        } catch (Exception ex) {
            log.error("Erreur sérialisation Kafka — topic={} : {}", topic, ex.getMessage());
        }
    }
}