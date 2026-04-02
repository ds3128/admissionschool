package org.darius.authservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.authservice.events.StudentAccountCreatedEvent;
import org.darius.authservice.events.UserActivatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static org.darius.authservice.kafka.KafkaConfig.TOPIC_USER_ACTIVATED;

@Component
@Slf4j
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishUserActivated(UserActivatedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC_USER_ACTIVATED, event.getUserId(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Échec envoi UserActivated : userId={} - {}",
                                    event.getUserId(), ex.getMessage());
                        } else {
                            log.info("UserActivated publié : userId={}, email={}, offset={}",
                                    event.getUserId(), event.getEmail(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception ex) {
            log.error("Erreur sérialisation UserActivated : {}", ex.getMessage());
        }
    }

//    public void publishUserActivated(UserActivatedEvent event) {
//        try {
//            String json = objectMapper.writeValueAsString(event);
//            kafkaTemplate.send(KafkaConfig.TOPIC_USER_ACTIVATED, event.getUserId(), json)
//                    .whenComplete((result, ex) -> {
//                        if (ex != null) {
//                            log.error("Échec envoi UserActivated - userId={} : {}",
//                                    event.getUserId(), ex.getMessage());
//                        } else {
//                            log.info("UserActivated publié - userId={}",
//                                    event.getUserId());
//                        }
//                    });
//        } catch (Exception ex) {
//            log.error("Erreur sérialisation UserActivated : {}", ex.getMessage());
//        }
//    }

    public void publishStudentAccountCreated(StudentAccountCreatedEvent event) {
        kafkaTemplate.send(KafkaConfig.TOPIC_STUDENT_ACCOUNT_CREATED, event.getUserId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Échec publication StudentAccountCreated — userId={} : {}",
                                event.getUserId(), ex.getMessage());
                    } else {
                        log.info("StudentAccountCreated publié — userId={}", event.getUserId());
                    }
                });
    }
}