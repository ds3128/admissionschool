package org.darius.payment.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.payment.events.consumed.SemesterValidatedEvent;
import org.darius.payment.services.ScholarshipService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final ScholarshipService scholarshipService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics           = KafkaConfig.TOPIC_SEMESTER_VALIDATED,
            groupId          = KafkaConfig.GROUP_PAYMENT_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSemesterValidated(String message, Acknowledgment ack) {
        log.info("SemesterValidated reçu : {}", message);
        try {
            SemesterValidatedEvent event = objectMapper.readValue(message, SemesterValidatedEvent.class);

            // Si c'est le dernier semestre de l'année → vérifier renouvellement bourses mérite
            if (event.isLastSemester()) {
                log.info("Dernier semestre de {} — déclenchement renouvellement bourses mérite",
                        event.getAcademicYear());
                scholarshipService.processMeritRenewalFromEvent(event);
            }

            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur traitement SemesterValidated : {}", ex.getMessage(), ex);
        }
    }
}