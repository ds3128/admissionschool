package org.darius.admission.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    // ── Topics consommés ─────────────────────────────────────────────────────
    public static final String TOPIC_PAYMENT_COMPLETED = "payment.completed";

    // ── Topics publiés ───────────────────────────────────────────────────────
    public static final String TOPIC_APPLICATION_SUBMITTED             = "application.submitted";
    public static final String TOPIC_APPLICATION_ADMIN_REVIEW          = "application.admin.review";
    public static final String TOPIC_APPLICATION_PENDING_COMMISSION    = "application.pending.commission";
    public static final String TOPIC_INTERVIEW_SCHEDULED               = "interview.scheduled";
    public static final String TOPIC_THESIS_APPROVAL_REQUESTED         = "thesis.approval.requested";
    public static final String TOPIC_APPLICATION_AWAITING_CONFIRMATION = "application.awaiting.confirmation";
    public static final String TOPIC_APPLICATION_ACCEPTED              = "application.accepted";
    public static final String TOPIC_APPLICATION_REJECTED              = "application.rejected";
    public static final String TOPIC_WAITLIST_PROMOTED                 = "waitlist.promoted";
    public static final String TOPIC_CHOICE_AUTO_CONFIRMED             = "choice.auto.confirmed";

    // ── Group ID ─────────────────────────────────────────────────────────────
    public static final String GROUP_ADMISSION_SERVICE = "admission-service-group";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ── Producer ──────────────────────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG,    "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ── Consumer ──────────────────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,       bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG,                GROUP_ADMISSION_SERVICE);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,  StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,      false);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConcurrency(3);
        return factory;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}