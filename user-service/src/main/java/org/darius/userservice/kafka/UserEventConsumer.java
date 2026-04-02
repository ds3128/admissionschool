package org.darius.userservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.userservice.common.dtos.CreateMinimalProfilRequest;
import org.darius.userservice.events.consumes.ApplicationAcceptedEvent;
import org.darius.userservice.events.consumes.SemesterValidatedEvent;
import org.darius.userservice.events.consumes.StudentPaymentBlockedEvent;
import org.darius.userservice.events.consumes.UserActivatedEvent;
import org.darius.userservice.services.StudentService;
import org.darius.userservice.services.UserProfileService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final UserProfileService userProfileService;
    private final StudentService     studentService;
    private final ObjectMapper       objectMapper;

    @KafkaListener(
            topics   = KafkaConfig.TOPIC_USER_ACTIVATED,
            groupId  = KafkaConfig.GROUP_USER_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserActivated(String message, Acknowledgment ack) {
        log.info("UserActivated reçu : {}", message);
        try {
            UserActivatedEvent event = objectMapper.readValue(message, UserActivatedEvent.class);

            // Creating minimal profile for user
            CreateMinimalProfilRequest request = CreateMinimalProfilRequest.builder()
                    .email(event.getEmail())
                    .firstName(event.getFirstName())
                    .lastName(event.getLastName())
                    .userId(event.getUserId())
                    .role(event.getRole())
                    .build();

            userProfileService.createMinimalProfile(request);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur traitement UserActivated : {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(
            topics   = KafkaConfig.TOPIC_APPLICATION_ACCEPTED,
            groupId  = KafkaConfig.GROUP_USER_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onApplicationAccepted(String message, Acknowledgment ack) {
        log.info("ApplicationAccepted reçu : {}", message);
        try {
            ApplicationAcceptedEvent event = objectMapper.readValue(message, ApplicationAcceptedEvent.class);
            studentService.createStudentFromAdmission(event);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur traitement ApplicationAccepted : {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(
            topics   = KafkaConfig.TOPIC_SEMESTER_VALIDATED,
            groupId  = KafkaConfig.GROUP_USER_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSemesterValidated(String message, Acknowledgment ack) {
        log.info("SemesterValidated reçu : {}", message);
        try {
            SemesterValidatedEvent event = objectMapper.readValue(message, SemesterValidatedEvent.class);
            event.getResults().stream()
                    .filter(SemesterValidatedEvent.StudentSemesterResult::isAdmis)
                    .forEach(r -> log.info("Étudiant admis : studentId={}", r.getStudentId()));
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur traitement SemesterValidated : {}", ex.getMessage(), ex);
        }
    }

    @KafkaListener(
            topics           = KafkaConfig.TOPIC_STUDENT_PAYMENT_BLOCKED,
            groupId          = KafkaConfig.GROUP_USER_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onStudentPaymentBlocked(String message, Acknowledgment ack) {
        log.info("StudentPaymentBlocked reçu : {}", message);
        try {
            StudentPaymentBlockedEvent event = objectMapper.readValue(
                    message, StudentPaymentBlockedEvent.class
            );
            userProfileService.blockStudent(
                    event.getUserId(),
                    "Impayé critique — " + event.getOverdueDays() + " jours de retard (" +
                            event.getAmount() + " " + "EUR)"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur traitement StudentPaymentBlocked : {}", ex.getMessage(), ex);
        }
    }
}