package org.darius.authservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.authservice.events.UserActivatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventProducer {

    private static final String TOPIC_USER_ACTIVATED = "user.activated";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishUserActivated(UserActivatedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC_USER_ACTIVATED, event.getUserId(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Échec envoi UserActivated : userId={} — {}",
                                    event.getUserId(), ex.getMessage());
                        } else {
                            log.info("UserActivated publié : userId={}, email={}",
                                    event.getUserId(), event.getEmail());
                        }
                    });
        } catch (Exception ex) {
            log.error("Erreur sérialisation UserActivated : {}", ex.getMessage());
        }
    }
}