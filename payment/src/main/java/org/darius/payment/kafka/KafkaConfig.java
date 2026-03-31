package org.darius.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

    // Topics consommés
    public static final String TOPIC_SEMESTER_VALIDATED = "semester.validated";

    // Topics publiés
    public static final String TOPIC_PAYMENT_COMPLETED         = "payment.completed";
    public static final String TOPIC_PAYMENT_FAILED            = "payment.failed";
    public static final String TOPIC_PAYMENT_REFUNDED          = "payment.refunded";
    public static final String TOPIC_INVOICE_GENERATED         = "invoice.generated";
    public static final String TOPIC_INVOICE_PAID              = "invoice.paid";
    public static final String TOPIC_INVOICE_OVERDUE           = "invoice.overdue";
    public static final String TOPIC_STUDENT_PAYMENT_BLOCKED   = "student.payment.blocked";
    public static final String TOPIC_SCHOLARSHIP_DISBURSED     = "scholarship.disbursed";
    public static final String TOPIC_SCHOLARSHIP_ACTIVATED     = "scholarship.activated";
    public static final String TOPIC_SCHOLARSHIP_SUSPENDED     = "scholarship.suspended";

    public static final String GROUP_PAYMENT_SERVICE = "payment-service-group";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,        bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,     StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG,   "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,        bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG,                 GROUP_PAYMENT_SERVICE);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,       false);
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
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}