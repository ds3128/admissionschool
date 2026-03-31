package org.darius.userservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.userservice.entities.Student;
import org.darius.userservice.entities.StudyLevel;
import org.darius.userservice.entities.UserProfile;
import org.darius.userservice.events.produces.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // ── Student ───────────────────────────────────────────────────────────────

    public void publishStudentProfileCreated(
            Student student,
            UserProfile profile,
            StudyLevel firstLevel
    ) {
        StudentProfileCreatedEvent event = StudentProfileCreatedEvent.builder()
                .studentId(student.getId())
                .userId(profile.getUserId())
                .filiereId(student.getFiliere().getId())
                .levelId(firstLevel.getId())
                .studentNumber(student.getStudentNumber())
                .academicYear(Year.now().getValue() + "-" + (Year.now().getValue() + 1))
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .build();

        send(KafkaConfig.TOPIC_STUDENT_PROFILE_CREATED, student.getId(), event);
    }

    public void publishStudentPromoted(StudentPromotedEvent event) {
        send(KafkaConfig.TOPIC_STUDENT_PROMOTED, event.getStudentId(), event);
    }

    public void publishStudentGraduated(StudentGraduatedEvent event) {
        send(KafkaConfig.TOPIC_STUDENT_GRADUATED, event.getStudentId(), event);
    }

    public void publishStudentTransferred(StudentTransferredEvent event) {
        send(KafkaConfig.TOPIC_STUDENT_TRANSFERRED, event.getStudentId(), event);
    }

    // ── Teacher ───────────────────────────────────────────────────────────────

    public void publishTeacherProfileCreated(TeacherProfileCreatedEvent event) {
        send(KafkaConfig.TOPIC_TEACHER_PROFILE_CREATED, event.getTeacherId(), event);
    }

    public void publishTeacherDeactivated(TeacherDeactivatedEvent event) {
        send(KafkaConfig.TOPIC_TEACHER_DEACTIVATED, event.getTeacherId(), event);
    }

    public void publishTeacherCreationRequested(TeacherCreationRequestedEvent event) {
        send(KafkaConfig.TOPIC_TEACHER_CREATION_REQUESTED, event.getRequestId(), event);
    }

    // ── Staff ─────────────────────────────────────────────────────────────────

    public void publishStaffProfileCreated(StaffProfileCreatedEvent event) {
        send(KafkaConfig.TOPIC_STAFF_PROFILE_CREATED, event.getStaffId(), event);
    }

    public void publishStaffCreationRequested(StaffCreationRequestedEvent event) {
        send(KafkaConfig.TOPIC_STAFF_CREATION_REQUESTED, event.getRequestId(), event);
    }

    public void publishStudentStatusChanged(StudentStatusChangedEvent event) {
        send(KafkaConfig.TOPIC_STUDENT_STATUS_CHANGED, event.getStudentId(), event);
    }


    // ── Helper ────────────────────────────────────────────────────────────────

    private void send(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Échec envoi Kafka - topic={}, key={} : {}", topic, key, ex.getMessage());
                        } else {
                            log.debug("Événement publié - topic={}, key={}", topic, key);
                        }
                    });
        } catch (Exception ex) {
            log.error("Erreur sérialisation Kafka - topic={} : {}", topic, ex.getMessage());
        }
    }
}