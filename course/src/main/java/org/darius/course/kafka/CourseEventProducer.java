package org.darius.course.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.course.events.published.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper                  objectMapper;

    public void publishStudentEnrolled(StudentEnrolledEvent event) {
        send(KafkaConfig.TOPIC_STUDENT_ENROLLED, event.getStudentId(), event);
    }

    public void publishAttendanceThresholdExceeded(AttendanceThresholdExceededEvent event) {
        send(KafkaConfig.TOPIC_ATTENDANCE_THRESHOLD_EXCEEDED, event.getStudentId(), event);
    }

    public void publishGradesPublished(GradesPublishedEvent event) {
        send(KafkaConfig.TOPIC_GRADES_PUBLISHED,
                String.valueOf(event.getEvaluationId()), event);
    }

    public void publishSessionCancelled(SessionCancelledEvent event) {
        send(KafkaConfig.TOPIC_SESSION_CANCELLED,
                String.valueOf(event.getSessionId()), event);
    }

    public void publishSemesterValidated(SemesterValidatedEvent event) {
        send(KafkaConfig.TOPIC_SEMESTER_VALIDATED,
                String.valueOf(event.getSemesterId()), event);
    }

    private void send(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Échec envoi Kafka — topic={}, key={} : {}",
                                    topic, key, ex.getMessage());
                        } else {
                            log.debug("Event publié — topic={}, key={}", topic, key);
                        }
                    });
        } catch (Exception ex) {
            log.error("Erreur sérialisation Kafka — topic={} : {}", topic, ex.getMessage());
        }
    }
}
