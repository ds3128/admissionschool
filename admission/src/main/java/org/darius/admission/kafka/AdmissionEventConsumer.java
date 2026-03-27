package org.darius.admission.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.admission.common.enums.ApplicationStatus;
import org.darius.admission.common.enums.PaymentStatus;
import org.darius.admission.evens.consumed.PaymentCompletedEvent;
import org.darius.admission.repositories.AdmissionPaymentRepository;
import org.darius.admission.repositories.ApplicationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdmissionEventConsumer {

    private final AdmissionPaymentRepository paymentRepository;
    private final ApplicationRepository applicationRepository;
    private final ObjectMapper               objectMapper;

    @KafkaListener(
            topics           = KafkaConfig.TOPIC_PAYMENT_COMPLETED,
            groupId          = KafkaConfig.GROUP_ADMISSION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentCompleted(String message, Acknowledgment ack) {
        log.info("PaymentCompleted reçu : {}", message);
        try {
            PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);

            // Retrouver le paiement via la référence ou l'applicationId
            paymentRepository.findByPaymentReference(event.getPaymentReference())
                    .or(() -> paymentRepository.findByApplication_Id(event.getApplicationId()))
                    .ifPresentOrElse(payment -> {
                        payment.setStatus(PaymentStatus.COMPLETED);
                        payment.setPaidAt(LocalDateTime.now());
                        payment.setPaymentReference(event.getPaymentReference());
                        paymentRepository.save(payment);

                        // Passer l'application en PAID
                        applicationRepository.findById(event.getApplicationId()).ifPresent(app -> {
                            if (app.getStatus() == ApplicationStatus.DRAFT) {
                                app.setStatus(ApplicationStatus.PAID);
                                app.setPaidAt(LocalDateTime.now());
                                applicationRepository.save(app);
                                log.info("Application {} → PAID", event.getApplicationId());
                            }
                        });

                        ack.acknowledge();
                    }, () -> {
                        log.warn("Paiement introuvable pour applicationId={}", event.getApplicationId());
                        ack.acknowledge(); // Acquitter pour ne pas bloquer — le paiement peut être externe
                    });

        } catch (Exception ex) {
            log.error("Erreur traitement PaymentCompleted : {}", ex.getMessage(), ex);
        }
    }
}