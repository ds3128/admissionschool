package org.darius.notification.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    // ── Topics Admission ──────────────────────────────────────────────────────
    public static final String TOPIC_APPLICATION_SUBMITTED              = "application.submitted";
    public static final String TOPIC_APPLICATION_ADMIN_REVIEW           = "application.admin.review";
    public static final String TOPIC_APPLICATION_PENDING_COMMISSION     = "application.pending.commission";
    public static final String TOPIC_INTERVIEW_SCHEDULED                = "interview.scheduled";
    public static final String TOPIC_THESIS_APPROVAL_REQUESTED          = "thesis.approval.requested";
    public static final String TOPIC_APPLICATION_AWAITING_CONFIRMATION  = "application.awaiting.confirmation";
    public static final String TOPIC_APPLICATION_ACCEPTED               = "application.accepted";
    public static final String TOPIC_APPLICATION_REJECTED               = "application.rejected";
    public static final String TOPIC_WAITLIST_PROMOTED                  = "waitlist.promoted";
    public static final String TOPIC_CHOICE_AUTO_CONFIRMED              = "choice.auto.confirmed";

    // ── Topics Payment ────────────────────────────────────────────────────────
    public static final String TOPIC_PAYMENT_COMPLETED                  = "payment.completed";
    public static final String TOPIC_PAYMENT_FAILED                     = "payment.failed";
    public static final String TOPIC_PAYMENT_REFUNDED                   = "payment.refunded";
    public static final String TOPIC_INVOICE_GENERATED                  = "invoice.generated";
    public static final String TOPIC_INVOICE_PAID                       = "invoice.paid";
    public static final String TOPIC_INVOICE_OVERDUE                    = "invoice.overdue";
    public static final String TOPIC_STUDENT_PAYMENT_BLOCKED            = "student.payment.blocked";
    public static final String TOPIC_SCHOLARSHIP_DISBURSED              = "scholarship.disbursed";

    // ── Topics Course ─────────────────────────────────────────────────────────
    public static final String TOPIC_STUDENT_ENROLLED                   = "student.enrolled";
    public static final String TOPIC_ATTENDANCE_THRESHOLD_EXCEEDED      = "attendance.threshold.exceeded";
    public static final String TOPIC_GRADES_PUBLISHED                   = "grades.published";
    public static final String TOPIC_SESSION_CANCELLED                  = "session.cancelled";
    public static final String TOPIC_SEMESTER_VALIDATED                 = "semester.validated";

    public static final String GROUP_NOTIFICATION_SERVICE = "notification-service-group";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,        bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG,                 GROUP_NOTIFICATION_SERVICE);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,       false);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,        "earliest");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConcurrency(3);
        return factory;
    }
}
