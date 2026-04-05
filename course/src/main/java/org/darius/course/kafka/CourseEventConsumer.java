package org.darius.course.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.UpdateGroupStudentsRequest;
import org.darius.course.events.consumed.*;
import org.darius.course.repositories.SemesterRepository;
import org.darius.course.services.EnrollmentService;
import org.darius.course.services.StudentGroupService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseEventConsumer {

    private final EnrollmentService    enrollmentService;
    private final StudentGroupService  groupService;
    private final SemesterRepository   semesterRepository;
    private final ObjectMapper         objectMapper;

    // ── StudentProfileCreated → groupes + enrollments ─────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_STUDENT_PROFILE_CREATED,
            groupId          = KafkaConfig.GROUP_COURSE_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onStudentProfileCreated(String message, Acknowledgment ack) {
        log.info("StudentProfileCreated reçu : {}", message);
        try {
            StudentProfileCreatedEvent event = objectMapper.readValue(
                    message, StudentProfileCreatedEvent.class
            );

            // Trouver le semestre actif
            semesterRepository.findByIsCurrent(true).ifPresentOrElse(semester -> {
                // Ajouter l'étudiant au groupe PROMO
                groupService.findOrCreatePromoGroup(
                        event.getLevelId(), event.getFiliereId(), semester.getId()
                );

                // Ajouter l'étudiant dans le groupe
                groupService.findOrCreatePromoGroup(
                        event.getLevelId(), event.getFiliereId(), semester.getId()
                );

                // Ajouter studentId dans le groupe puis créer les enrollments
                semesterRepository.findByIsCurrent(true).ifPresent(s -> {
                    var group = groupService.findOrCreatePromoGroup(
                            event.getLevelId(), event.getFiliereId(), s.getId()
                    );
                    // Ajouter l'étudiant dans le groupe
                    try {
                        var req = new UpdateGroupStudentsRequest();
                        req.setStudentIds(List.of(event.getStudentId()));
                        req.setAction("ADD");
                        groupService.updateStudents(group.getId(), req);
                    } catch (Exception e) {
                        log.warn("Étudiant déjà dans le groupe : {}", e.getMessage());
                    }
                    enrollmentService.createEnrollmentsForStudent(
                            event.getStudentId(), event.getLevelId(), s.getId()
                    );
                });

            }, () -> log.warn(
                    "Aucun semestre actif - enrollment différé pour studentId={}",
                    event.getStudentId()
            ));

            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur traitement StudentProfileCreated : {}", ex.getMessage(), ex);
        }
    }

    // ── StudentPromoted → mise à jour groupes ─────────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_STUDENT_PROMOTED,
            groupId          = KafkaConfig.GROUP_COURSE_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onStudentPromoted(String message, Acknowledgment ack) {
        log.info("StudentPromoted reçu : {}", message);
        try {
            StudentPromotedEvent event = objectMapper.readValue(
                    message, StudentPromotedEvent.class
            );

            semesterRepository.findByIsCurrent(true).ifPresent(semester -> {
                // Ajouter l'étudiant au groupe du nouveau niveau
                var newGroup = groupService.findOrCreatePromoGroup(
                        event.getNewLevelId(), event.getFiliereId(), semester.getId()
                );

                var addReq = new UpdateGroupStudentsRequest();
                addReq.setStudentIds(java.util.List.of(event.getStudentId()));
                addReq.setAction("ADD");
                groupService.updateStudents(newGroup.getId(), addReq);

                // Créer les nouveaux enrollments pour le niveau promu
                enrollmentService.createEnrollmentsForStudent(
                        event.getStudentId(), event.getNewLevelId(), semester.getId()
                );

                log.info("Étudiant {} promu au niveau {} dans le groupe {}",
                        event.getStudentId(), event.getNewLevelId(), newGroup.getId());
            });

            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur traitement StudentPromoted : {}", ex.getMessage(), ex);
        }
    }

    // ── StudentPaymentBlocked → Enrollment BLOCKED ────────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_STUDENT_PAYMENT_BLOCKED,
            groupId          = KafkaConfig.GROUP_COURSE_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onStudentPaymentBlocked(String message, Acknowledgment ack) {
        log.info("StudentPaymentBlocked reçu : {}", message);
        try {
            StudentPaymentBlockedEvent event = objectMapper.readValue(
                    message, StudentPaymentBlockedEvent.class
            );

            semesterRepository.findByIsCurrent(true).ifPresent(semester ->
                    enrollmentService.blockStudentEnrollments(
                            event.getStudentId(), semester.getId()
                    )
            );

            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur traitement StudentPaymentBlocked : {}", ex.getMessage(), ex);
        }
    }

    // ── TeacherProfileCreated → enseignant disponible ────────────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_TEACHER_PROFILE_CREATED,
            groupId          = KafkaConfig.GROUP_COURSE_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTeacherProfileCreated(String message, Acknowledgment ack) {
        log.info("TeacherProfileCreated reçu : {}", message);
        try {
            TeacherProfileCreatedEvent event = objectMapper.readValue(
                    message, TeacherProfileCreatedEvent.class
            );
            // L'enseignant est maintenant disponible pour les affectations
            // Pas d'action directe requise — les affectations sont créées manuellement
            log.info("Enseignant {} disponible pour affectations (dept={})",
                    event.getTeacherId(), event.getDepartmentId());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur traitement TeacherProfileCreated : {}", ex.getMessage(), ex);
        }
    }

    // ── TeacherDeactivated → retirer des affectations futures ────────────────
    @KafkaListener(
            topics           = KafkaConfig.TOPIC_TEACHER_DEACTIVATED,
            groupId          = KafkaConfig.GROUP_COURSE_SERVICE,
            containerFactory = "kafkaListenerContainerFactory"
    )
//    @RetryableTopic(
//            attempts = "5",
//            dltStrategy = DltStrategy.FAIL_ON_ERROR,
//            backOff =  @BackOff(delay = 5000, multiplier = 2.0),
//            include = {RuntimeException.class, JsonProcessingException.class}
//    )
    public void onTeacherDeactivated(String message, Acknowledgment ack) {
        log.info("TeacherDeactivated reçu : {}", message);
        try {
            TeacherDeactivatedEvent event = objectMapper.readValue(
                    message, TeacherDeactivatedEvent.class
            );
            // TODO : implémenter la logique de désactivation des affectations futures
            // Pour l'instant : log d'information
            log.warn("Enseignant {} désactivé - affectations futures à vérifier (raison: {})",
                    event.getTeacherId(), event.getReason());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Erreur traitement TeacherDeactivated : {}", ex.getMessage(), ex);
        }
    }
}