package org.darius.userservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.userservice.events.consumes.ApplicationAcceptedEvent;
import org.darius.userservice.events.consumes.SemesterValidatedEvent;
import org.darius.userservice.events.consumes.UserActivatedEvent;
import org.darius.userservice.services.StudentService;
import org.darius.userservice.services.UserProfileService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final UserProfileService userProfileService;
    private final StudentService     studentService;

    // ── UserActivated → Crée un profil minimal ────────────────────────────────

    @KafkaListener(
            topics   = KafkaConfig.TOPIC_USER_ACTIVATED,
            groupId  = KafkaConfig.GROUP_USER_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserActivated(
            @Payload UserActivatedEvent event,
            Acknowledgment ack
    ) {
        log.info("UserActivated reçu : userId={}, role={}", event.getUserId(), event.getRole());
        try {
            // Crée uniquement un profil minimal — ne crée PAS de Student/Teacher/Staff
            // La création complète se fait via ApplicationAccepted ou manuellement
            userProfileService.createMinimalProfile(event.getUserId(), event.getEmail());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur traitement UserActivated : userId={} — {}",
                    event.getUserId(), ex.getMessage(), ex);
            // Ne pas acquitter → Kafka relivrera le message
        }
    }

    // ── ApplicationAccepted → Crée profil étudiant complet ───────────────────

    @KafkaListener(
            topics   = KafkaConfig.TOPIC_APPLICATION_ACCEPTED,
            groupId  = KafkaConfig.GROUP_USER_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onApplicationAccepted(
            @Payload ApplicationAcceptedEvent event,
            Acknowledgment ack
    ) {
        log.info("ApplicationAccepted reçu : userId={}, filiereId={}",
                event.getUserId(), event.getFiliereId());
        try {
            studentService.createStudentFromAdmission(event);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur traitement ApplicationAccepted : userId={} — {}",
                    event.getUserId(), ex.getMessage(), ex);
            // Ne pas acquitter → Dead Letter Queue après N retries
        }
    }

    // ── SemesterValidated → Mise à jour progression étudiants ────────────────

    @KafkaListener(
            topics   = KafkaConfig.TOPIC_SEMESTER_VALIDATED,
            groupId  = KafkaConfig.GROUP_USER_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSemesterValidated(
            @Payload SemesterValidatedEvent event,
            Acknowledgment ack
    ) {
        log.info("SemesterValidated reçu : semesterId={}, {} étudiants",
                event.getSemesterId(), event.getResults().size());
        try {
            // Pour chaque étudiant ADMIS → promotion automatique si dernier niveau non atteint
            // L'admin reste responsable de la décision finale — ce consumer logue uniquement
            event.getResults().stream()
                    .filter(SemesterValidatedEvent.StudentSemesterResult::isAdmis)
                    .forEach(result -> log.info(
                            "Étudiant admis : studentId={}, average={}",
                            result.getStudentId(), result.getAverage()
                    ));
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur traitement SemesterValidated : semesterId={} — {}",
                    event.getSemesterId(), ex.getMessage(), ex);
        }
    }
}