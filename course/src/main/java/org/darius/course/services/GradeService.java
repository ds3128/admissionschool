package org.darius.course.services;

import org.darius.course.dtos.requests.SubmitGradesRequest;
import org.darius.course.dtos.responses.GradeResponse;
import java.util.List;

public interface GradeService {

    /**
     * Saisit les notes d'une évaluation.
     * Valide que le semestre est ACTIVE.
     * Recalcule ClassStats.
     */
    List<GradeResponse> submitGrades(Long evaluationId, String teacherId, SubmitGradesRequest request);

    GradeResponse updateGrade(Long gradeId, String teacherId, double score, String comment);

    GradeResponse getById(Long id);

    List<GradeResponse> getByStudentAndSemester(String studentId, Long semesterId);

    List<GradeResponse> getByStudentAndMatiereAndSemester(
            String studentId, Long matiereId, Long semesterId
    );

    List<GradeResponse> getByEvaluation(Long evaluationId);
}
