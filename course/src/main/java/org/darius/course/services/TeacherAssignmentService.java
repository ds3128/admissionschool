package org.darius.course.services;

import org.darius.course.dtos.requests.CreateTeacherAssignmentRequest;
import org.darius.course.dtos.responses.TeacherAssignmentResponse;
import org.darius.course.dtos.responses.TeacherLoadResponse;
import java.util.List;

public interface TeacherAssignmentService {

    TeacherAssignmentResponse create(CreateTeacherAssignmentRequest request);

    void delete(Long id);

    List<TeacherAssignmentResponse> getByTeacherAndSemester(String teacherId, Long semesterId);

    List<TeacherAssignmentResponse> getByMatiereAndSemester(Long matiereId, Long semesterId);

    /** Charge horaire agrégée d'un enseignant pour un semestre. */
    TeacherLoadResponse getTeacherLoad(String teacherId, Long semesterId);

    /**
     * Vérifie si un enseignant est affecté à une matière pour un semestre.
     * Utilisé pour les vérifications d'autorisation dans les controllers.
     */
    boolean isTeacherAssigned(String teacherId, Long matiereId, Long semesterId);
}
