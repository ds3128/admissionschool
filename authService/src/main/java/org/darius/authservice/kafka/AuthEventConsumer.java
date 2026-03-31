package org.darius.authservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.authservice.common.enums.RoleType;
import org.darius.authservice.entities.Role;
import org.darius.authservice.entities.Users;
import org.darius.authservice.events.ApplicationAcceptedEvent;
import org.darius.authservice.repositories.UserRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consommateur Kafka de l'Auth Service.
 * <p>
 * Quand une candidature est acceptée (ApplicationAccepted), le compte existant
 * du candidat (rôle CANDIDATE) est promu au rôle STUDENT. C'est le seul
 * changement nécessaire côté Auth — le profil étudiant est créé par le User Service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventConsumer {

    private final UserRepository userRepository;

    @KafkaListener(
            topics           = KafkaConfig.TOPIC_APPLICATION_ACCEPTED,
            groupId          = KafkaConfig.GROUP_AUTH_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onApplicationAccepted(
            @Payload ApplicationAcceptedEvent event,
            Acknowledgment ack
    ) {
        log.info("ApplicationAccepted reçu par Auth Service : userId={}, studentNumber={}",
                event.getUserId(), event.getStudentNumber());
        try {
            Users user = userRepository.findById(event.getUserId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Utilisateur introuvable pour userId=" + event.getUserId()
                    ));

            // Promotion CANDIDATE → STUDENT
            Role role = user.getRole();
            if (role.getRoleType() != RoleType.STUDENT) {
                role.setRoleType(RoleType.STUDENT);
                userRepository.save(user);
                log.info("Rôle promu CANDIDATE → STUDENT pour userId={}", event.getUserId());
            } else {
                log.warn("Utilisateur userId={} déjà STUDENT — événement ignoré", event.getUserId());
            }

            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur traitement ApplicationAccepted (auth) : userId={} — {}",
                    event.getUserId(), ex.getMessage(), ex);
            // Ne pas acquitter → Kafka relivrera le message
        }
    }
}