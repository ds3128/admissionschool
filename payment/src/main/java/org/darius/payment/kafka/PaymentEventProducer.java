package org.darius.payment.kafka;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.payment.events.published.*;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        send(KafkaConfig.TOPIC_PAYMENT_COMPLETED, event.getPaymentReference(), event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        send(KafkaConfig.TOPIC_PAYMENT_FAILED, event.getPaymentReference(), event);
    }

    public void publishPaymentRefunded(PaymentRefundedEvent event) {
        send(KafkaConfig.TOPIC_PAYMENT_REFUNDED, event.getOriginalPaymentReference(), event);
    }

    public void publishInvoiceGenerated(InvoiceGeneratedEvent event) {
        send(KafkaConfig.TOPIC_INVOICE_GENERATED, event.getInvoiceId(), event);
    }

    public void publishInvoicePaid(InvoicePaidEvent event) {
        send(KafkaConfig.TOPIC_INVOICE_PAID, event.getInvoiceId(), event);
    }

    public void publishInvoiceOverdue(InvoiceOverdueEvent event) {
        send(KafkaConfig.TOPIC_INVOICE_OVERDUE, event.getInvoiceId(), event);
    }

    public void publishStudentPaymentBlocked(StudentPaymentBlockedEvent event) {
        send(KafkaConfig.TOPIC_STUDENT_PAYMENT_BLOCKED, event.getStudentId(), event);
    }

    public void publishScholarshipDisbursed(ScholarshipDisbursedEvent event) {
        send(KafkaConfig.TOPIC_SCHOLARSHIP_DISBURSED, event.getStudentId(), event);
    }

    public void publishScholarshipActivated(ScholarshipActivatedEvent event) {
        send(KafkaConfig.TOPIC_SCHOLARSHIP_ACTIVATED, event.getStudentId(), event);
    }

    public void publishScholarshipSuspended(ScholarshipSuspendedEvent event) {
        send(KafkaConfig.TOPIC_SCHOLARSHIP_SUSPENDED, event.getStudentId(), event);
    }

    private void send(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Échec envoi Kafka — topic={}, key={} : {}", topic, key, ex.getMessage());
                        } else {
                            log.debug("Event publié — topic={}, key={}", topic, key);
                        }
                    });
        } catch (Exception ex) {
            log.error("Erreur sérialisation Kafka — topic={} : {}", topic, ex.getMessage());
        }
    }
}
