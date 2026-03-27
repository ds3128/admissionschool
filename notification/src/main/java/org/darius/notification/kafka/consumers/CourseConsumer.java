package org.darius.notification.kafka.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.notification.enums.NotificationType;
import org.darius.notification.events.course.*;
import org.darius.notification.kafka.KafkaConfig;
import org.darius.notification.mail.UserResolverService;
import org.darius.notification.services.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseConsumer {

    private final NotificationService notificationService;
    private final UserResolverService userResolver;
    private final ObjectMapper        objectMapper;

    // ── student.enrolled ──────────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_STUDENT_ENROLLED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onStudentEnrolled(String message, Acknowledgment ack) {
        log.info("student.enrolled reçu");
        try {
            var event = objectMapper.readValue(message, StudentEnrolledEvent.class);

            String email = userResolver.resolveEmail(event.getStudentId());
            if (email == null) {
                ack.acknowledge();
                return;
            }

            String firstName = userResolver.resolveFirstName(event.getStudentId());

            notificationService.send(
                    event.getStudentId(),
                    email,
                    NotificationType.STUDENT_ENROLLED,
                    "Inscriptions aux cours confirmées",
                    "course/student-enrolled",
                    Map.of(
                            "firstName",     firstName != null ? firstName : "Étudiant",
                            "semesterLabel", event.getSemesterLabel(),
                            "academicYear",  event.getAcademicYear(),
                            "matiereNames",  event.getMatiereNames()
                    ),
                    String.valueOf(event.getSemesterId()), "SEMESTER"
            );
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur student.enrolled : {}", ex.getMessage(), ex);
        }
    }

    // ── attendance.threshold.exceeded ─────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_ATTENDANCE_THRESHOLD_EXCEEDED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAttendanceThresholdExceeded(String message, Acknowledgment ack) {
        log.info("attendance.threshold.exceeded reçu");
        try {
            var event = objectMapper.readValue(message, AttendanceThresholdExceededEvent.class);

            String email     = userResolver.resolveEmail(event.getStudentId());
            String firstName = userResolver.resolveFirstName(event.getStudentId());

            if (email == null) {
                ack.acknowledge();
                return;
            }

            // Notification à l'étudiant
            notificationService.send(
                    event.getStudentId(),
                    email,
                    NotificationType.ATTENDANCE_THRESHOLD_EXCEEDED,
                    "⚠️ Seuil d'absences dépassé — " + event.getMatiereName(),
                    "course/attendance-threshold-exceeded",
                    Map.of(
                            "firstName",      firstName != null ? firstName : "Étudiant",
                            "matiereName",    event.getMatiereName(),
                            "attendanceRate", String.format("%.1f", event.getAttendanceRate()),
                            "threshold",      String.format("%.0f", event.getThreshold()),
                            "absenceCount",   event.getAbsenceCount(),
                            "totalSessions",  event.getTotalSessions()
                    ),
                    String.valueOf(event.getMatiereId()), "MATIERE"
            );

            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur attendance.threshold.exceeded : {}", ex.getMessage(), ex);
        }
    }

    // ── grades.published ──────────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_GRADES_PUBLISHED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onGradesPublished(String message, Acknowledgment ack) {
        log.info("grades.published reçu");
        try {
            var event = objectMapper.readValue(message, GradesPublishedEvent.class);

            if (event.getStudentIds() == null || event.getStudentIds().isEmpty()) {
                ack.acknowledge();
                return;
            }

            // Envoi bulk — un email par étudiant
            for (String studentId : event.getStudentIds()) {
                String email     = userResolver.resolveEmail(studentId);
                String firstName = userResolver.resolveFirstName(studentId);

                if (email == null) continue;

                notificationService.send(
                        studentId,
                        email,
                        NotificationType.GRADES_PUBLISHED,
                        "Vos notes sont disponibles — " + event.getMatiereName(),
                        "course/grades-published",
                        Map.of(
                                "firstName",       firstName != null ? firstName : "Étudiant",
                                "evaluationTitle", event.getEvaluationTitle(),
                                "matiereName",     event.getMatiereName()
                        ),
                        String.valueOf(event.getEvaluationId()), "EVALUATION"
                );
            }

            log.info("grades.published : {} emails envoyés pour évaluation {}",
                    event.getStudentIds().size(), event.getEvaluationTitle());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur grades.published : {}", ex.getMessage(), ex);
        }
    }

    // ── session.cancelled ─────────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_SESSION_CANCELLED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSessionCancelled(String message, Acknowledgment ack) {
        log.info("session.cancelled reçu");
        try {
            var event = objectMapper.readValue(message, SessionCancelledEvent.class);

            if (event.getAffectedStudentIds() == null || event.getAffectedStudentIds().isEmpty()) {
                ack.acknowledge();
                return;
            }

            // Envoi bulk — un email par étudiant du groupe
            for (String studentId : event.getAffectedStudentIds()) {
                String email     = userResolver.resolveEmail(studentId);
                String firstName = userResolver.resolveFirstName(studentId);

                if (email == null) continue;

                notificationService.send(
                        studentId,
                        email,
                        NotificationType.SESSION_CANCELLED,
                        "Séance annulée — " + event.getMatiereName() + " le " + event.getDate(),
                        "course/session-cancelled",
                        Map.of(
                                "firstName",   firstName != null ? firstName : "Étudiant",
                                "matiereName", event.getMatiereName(),
                                "date",        event.getDate(),
                                "startTime",   event.getStartTime(),
                                "reason",      event.getReason()
                        ),
                        String.valueOf(event.getSessionId()), "SESSION"
                );
            }

            log.info("session.cancelled : {} emails envoyés pour session {}",
                    event.getAffectedStudentIds().size(), event.getSessionId());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur session.cancelled : {}", ex.getMessage(), ex);
        }
    }

    // ── semester.validated ────────────────────────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_SEMESTER_VALIDATED,
            groupId          = KafkaConfig.GROUP_NOTIFICATION_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSemesterValidated(String message, Acknowledgment ack) {
        log.info("semester.validated reçu");
        try {
            var event = objectMapper.readValue(message, SemesterValidatedEvent.class);

            if (event.getResults() == null || event.getResults().isEmpty()) {
                ack.acknowledge();
                return;
            }

            // Envoi bulk — un email personnalisé par étudiant
            for (SemesterValidatedEvent.StudentResult result : event.getResults()) {
                String email     = userResolver.resolveEmail(result.getStudentId());
                String firstName = userResolver.resolveFirstName(result.getStudentId());

                if (email == null) continue;

                // Template différent selon statut ADMIS ou AJOURNE
                String template = result.isAdmis()
                        ? "course/semester-validated-admis"
                        : "course/semester-validated-ajourne";

                String subject = result.isAdmis()
                        ? "Résultats du semestre — Félicitations !"
                        : "Résultats du semestre — " + event.getSemesterLabel();

                Map<String, Object> templateData = result.isAdmis()
                        ? Map.of(
                        "firstName",     firstName != null ? firstName : "Étudiant",
                        "semesterLabel", event.getSemesterLabel(),
                        "average",       String.format("%.2f", result.getSemesterAverage()),
                        "mention",       result.getMention(),
                        "rank",          result.getRank(),
                        "credits",       result.getCreditsObtained()
                )
                        : Map.of(
                        "firstName",     firstName != null ? firstName : "Étudiant",
                        "semesterLabel", event.getSemesterLabel(),
                        "average",       String.format("%.2f", result.getSemesterAverage()),
                        "credits",       result.getCreditsObtained()
                );

                notificationService.send(
                        result.getStudentId(),
                        email,
                        NotificationType.SEMESTER_VALIDATED,
                        subject,
                        template,
                        templateData,
                        String.valueOf(event.getSemesterId()), "SEMESTER"
                );
            }

            log.info("semester.validated : {} emails envoyés pour semestre {}",
                    event.getResults().size(), event.getSemesterLabel());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur semester.validated : {}", ex.getMessage(), ex);
        }
    }
}
