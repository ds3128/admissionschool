package org.darius.notification.kafka.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.notification.enums.NotificationType;
import org.darius.notification.events.payment.*;
import org.darius.notification.kafka.KafkaConfig;
import org.darius.notification.mail.UserResolverService;
import org.darius.notification.services.NotificationService;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final NotificationService notificationService;
    private final UserResolverService userResolver;
    private final ObjectMapper        objectMapper;

    // ── payment.completed ─────────────────────────────────────────────────────
//    @KafkaListener(
//            topics           = KafkaConfig.TOPIC_PAYMENT_COMPLETED,
//            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void onPaymentCompleted(String message, Acknowledgment ack) {
//        log.info("payment.completed reçu");
//        try {
//            var event = objectMapper.readValue(message, PaymentCompletedEvent.class);
//
//            // Résolution email si absent
//            String email = userResolver.resolveEmail(event.getUserId());
//            if (email == null) {
//                log.warn("Email introuvable pour userId={}", event.getUserId());
//                ack.acknowledge();
//                return;
//            }
//
//            notificationService.send(
//                    event.getUserId(),
//                    email,
//                    NotificationType.PAYMENT_COMPLETED,
//                    "Paiement confirmé — Reçu de paiement",
//                    "mail/payment/payment-completed",
//                    Map.of(
//                            "paymentReference", event.getPaymentReference(),
//                            "amount",           event.getAmount(),
//                            "currency",         event.getCurrency(),
//                            "type",             event.getType(),
//                            "paidAt",           event.getPaidAt()
//                    ),
//                    event.getPaymentReference(), "PAYMENT"
//            );
//            ack.acknowledge();
//        } catch (Exception ex) {
//            log.error("Erreur payment.completed : {}", ex.getMessage(), ex);
//        }
//    }
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_PAYMENT_COMPLETED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentCompleted(String message, Acknowledgment ack) {
        log.info("payment.completed reçu");
        try {
            var event = objectMapper.readValue(message, PaymentCompletedEvent.class);

            UserResolverService.UserInfo userInfo = userResolver.resolve(event.getUserId());
            if (userInfo == null || userInfo.email() == null) {
                log.warn("Email introuvable pour userId={}", event.getUserId());
                ack.acknowledge();
                return;
            }

            Map<String, Object> vars = getStringObjectMap(event, userInfo);

            notificationService.send(
                    event.getUserId(),
                    userInfo.email(),
                    NotificationType.PAYMENT_COMPLETED,
                    "Paiement confirmé - Reçu de paiement",
                    "mail/payment/payment-completed",
                    vars,
                    event.getPaymentReference(), "PAYMENT"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur payment.completed : {}", ex.getMessage(), ex);
        }
    }

    private static @NonNull Map<String, Object> getStringObjectMap(PaymentCompletedEvent event, UserResolverService.UserInfo userInfo) {
        String description = "FRAIS_DOSSIER".equals(event.getType())   ? "Frais de dossier"  :
                "FRAIS_SCOLARITE".equals(event.getType())  ? "Frais de scolarité" :
                "BOURSE".equals(event.getType())           ? "Bourse"             :
                event.getType() != null ? event.getType() : "";

        Map<String, Object> vars = new java.util.HashMap<>();
        vars.put("firstName",     userInfo.firstName()            != null ? userInfo.firstName()            : "");
        vars.put("transactionId", event.getPaymentReference()     != null ? event.getPaymentReference()     : "");
        vars.put("invoiceNumber", event.getInvoiceId()            != null ? event.getInvoiceId()            : "");
        vars.put("description",   description);
        vars.put("paymentMethod", event.getType()                 != null ? event.getType()                 : "");
        vars.put("paidAt",        event.getPaidAt()               != null ? event.getPaidAt()               : "");
        vars.put("amount",        event.getAmount());
        vars.put("currency",      event.getCurrency()             != null ? event.getCurrency()             : "XAF");
        return vars;
    }


    // ── payment.failed ────────────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_PAYMENT_FAILED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentFailed(String message, Acknowledgment ack) {
        log.info("payment.failed reçu");
        try {
            var event = objectMapper.readValue(message, PaymentFailedEvent.class);

            String email = userResolver.resolveEmail(event.getUserId());
            if (email == null) {
                ack.acknowledge();
                return;
            }

            notificationService.send(
                    event.getUserId(),
                    email,
                    NotificationType.PAYMENT_FAILED,
                    "Échec du paiement — Action requise",
                    "mail/payment/payment-failed",
                    Map.of(
                            "paymentReference", event.getPaymentReference(),
                            "amount",           event.getAmount(),
                            "currency",         event.getCurrency(),
                            "failureReason",    event.getFailureReason() != null
                                    ? event.getFailureReason() : "Raison inconnue"
                    ),
                    event.getPaymentReference(), "PAYMENT"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur payment.failed : {}", ex.getMessage(), ex);
        }
    }

    // ── payment.refunded ──────────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_PAYMENT_REFUNDED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentRefunded(String message, Acknowledgment ack) {
        log.info("payment.refunded reçu");
        try {
            var event = objectMapper.readValue(message, PaymentRefundedEvent.class);

            String email = userResolver.resolveEmail(event.getUserId());
            if (email == null) {
                ack.acknowledge();
                return;
            }

            notificationService.send(
                    event.getUserId(),
                    email,
                    NotificationType.PAYMENT_REFUNDED,
                    "Remboursement effectué",
                    "mail/payment/payment-refunded",
                    Map.of(
                            "originalReference", event.getOriginalPaymentReference(),
                            "refundReference",   event.getRefundPaymentReference(),
                            "amount",            event.getAmount(),
                            "reason",            event.getReason()
                    ),
                    event.getOriginalPaymentReference(), "PAYMENT"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur payment.refunded : {}", ex.getMessage(), ex);
        }
    }

    // ── invoice.generated ────────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_INVOICE_GENERATED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onInvoiceGenerated(String message, Acknowledgment ack) {
        log.info("invoice.generated reçu");
        try {
            var event = objectMapper.readValue(message, InvoiceGeneratedEvent.class);

            String email = userResolver.resolveEmail(event.getStudentId());
            if (email == null) {
                ack.acknowledge();
                return;
            }

            notificationService.send(
                    event.getStudentId(),
                    email,
                    NotificationType.INVOICE_GENERATED,
                    "Nouvelle facture disponible",
                    "mail/payment/invoice-created",
                    Map.of(
                            "academicYear",         event.getAcademicYear(),
                            "semester",             event.getSemester(),
                            "netAmount",            event.getNetAmount(),
                            "scholarshipDeduction", event.getScholarshipDeduction(),
                            "dueDate",              event.getDueDate()
                    ),
                    event.getInvoiceId(), "INVOICE"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur invoice.generated : {}", ex.getMessage(), ex);
        }
    }

    // ── invoice.paid ──────────────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_INVOICE_PAID,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onInvoicePaid(String message, Acknowledgment ack) {
        log.info("invoice.paid reçu");
        try {
            var event = objectMapper.readValue(message, InvoicePaidEvent.class);

            String email = userResolver.resolveEmail(event.getStudentId());
            if (email == null) {
                ack.acknowledge();
                return;
            }

            notificationService.send(
                    event.getStudentId(),
                    email,
                    NotificationType.INVOICE_PAID,
                    "Facture réglée — Merci",
                    "mail/payment/invoice-paid",
                    Map.of(
                            "academicYear", event.getAcademicYear(),
                            "amount",       event.getAmount()
                    ),
                    event.getInvoiceId(), "INVOICE"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur invoice.paid : {}", ex.getMessage(), ex);
        }
    }

    // ── invoice.overdue ───────────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_INVOICE_OVERDUE,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onInvoiceOverdue(String message, Acknowledgment ack) {
        log.info("invoice.overdue reçu");
        try {
            var event = objectMapper.readValue(message, InvoiceOverdueEvent.class);

            String email = userResolver.resolveEmail(event.getStudentId());
            if (email == null) {
                ack.acknowledge();
                return;
            }

            // CRITIQUE — toujours envoyé
            notificationService.sendCritical(
                    event.getStudentId(),
                    email,
                    NotificationType.INVOICE_OVERDUE,
                    "⚠️ Facture en retard - Régularisez votre situation",
                    "mail/payment/invoice-overdue",
                    Map.of(
                            "remainingAmount", event.getRemainingAmount(),
                            "dueDate",         event.getDueDate(),
                            "academicYear",    event.getAcademicYear()
                    ),
                    event.getInvoiceId(), "INVOICE"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur invoice.overdue : {}", ex.getMessage(), ex);
        }
    }

    // ── student.payment.blocked ───────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_STUDENT_PAYMENT_BLOCKED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onStudentPaymentBlocked(String message, Acknowledgment ack) {
        log.info("student.payment.blocked reçu");
        try {
            var event = objectMapper.readValue(message, StudentPaymentBlockedEvent.class);

            // Utiliser userId si présent, sinon studentId
            String resolveId = event.getUserId() != null
                    ? event.getUserId() : event.getStudentId();
            String email = userResolver.resolveEmail(resolveId);
            if (email == null) {
                ack.acknowledge();
                return;
            }

            // CRITIQUE — toujours envoyé
            notificationService.sendCritical(
                    resolveId,
                    email,
                    NotificationType.STUDENT_PAYMENT_BLOCKED,
                    "🔒 Accès restreint - Impayé critique",
                    "mail/payment/student-payment-blocked",
                    Map.of(
                            "amount",      event.getAmount(),
                            "overdueDays", event.getOverdueDays(),
                            "academicYear",event.getAcademicYear()
                    ),
                    event.getInvoiceId(), "INVOICE"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur student.payment.blocked : {}", ex.getMessage(), ex);
        }
    }

    // ── scholarship.disbursed ────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_SCHOLARSHIP_DISBURSED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onScholarshipDisbursed(String message, Acknowledgment ack) {
        log.info("scholarship.disbursed reçu");
        try {
            var event = objectMapper.readValue(message, ScholarshipDisbursedEvent.class);

            String email = userResolver.resolveEmail(event.getStudentId());
            if (email == null) {
                ack.acknowledge();
                return;
            }

            notificationService.send(
                    event.getStudentId(),
                    email,
                    NotificationType.SCHOLARSHIP_DISBURSED,
                    "Versement de bourse effectué",
                    "mail/payment/scholarship-disbursement",
                    Map.of(
                            "amount",           event.getAmount(),
                            "period",           event.getPeriod(),
                            "paymentReference", event.getPaymentReference()
                    ),
                    String.valueOf(event.getScholarshipId()), "SCHOLARSHIP"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur scholarship.disbursed : {}", ex.getMessage(), ex);
        }
    }
}
