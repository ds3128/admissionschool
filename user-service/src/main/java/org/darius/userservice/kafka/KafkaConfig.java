package org.darius.userservice.kafka;

import org.springframework.context.annotation.Configuration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    // ── Topic names ───────────────────────────────────────────────────────────

    // Consommés
    public static final String TOPIC_USER_ACTIVATED         = "user.activated";
    public static final String TOPIC_APPLICATION_ACCEPTED   = "application.accepted";
    public static final String TOPIC_SEMESTER_VALIDATED     = "semester.validated";

    // Publiés
    public static final String TOPIC_STUDENT_PROFILE_CREATED    = "student.profile.created";
    public static final String TOPIC_TEACHER_PROFILE_CREATED    = "teacher.profile.created";
    public static final String TOPIC_STAFF_PROFILE_CREATED      = "staff.profile.created";
    public static final String TOPIC_STUDENT_PROMOTED           = "student.promoted";
    public static final String TOPIC_STUDENT_GRADUATED          = "student.graduated";
    public static final String TOPIC_STUDENT_TRANSFERRED        = "student.transferred";
    public static final String TOPIC_TEACHER_DEACTIVATED        = "teacher.deactivated";
    public static final String TOPIC_TEACHER_CREATION_REQUESTED = "teacher.creation.requested";
    public static final String TOPIC_STAFF_CREATION_REQUESTED   = "staff.creation.requested";

    // Group ID
    public static final String GROUP_USER_SERVICE = "user-service-group";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ── Producer ──────────────────────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerde.class);
        // Garantit l'ordre des messages par partition
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ── Consumer ──────────────────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_USER_SERVICE);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        // Désactive l'auto-commit — on acquitte manuellement (Acknowledgment)
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "org.darius.*");
        config.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.Map");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Acquittement manuel — le consumer appelle ack.acknowledge() explicitement
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConcurrency(3);
        return factory;
    }
}
