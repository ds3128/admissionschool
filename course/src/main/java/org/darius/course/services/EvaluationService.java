package org.darius.course.services;

import org.darius.course.dtos.requests.CreateEvaluationRequest;
import org.darius.course.dtos.responses.ClassStatsResponse;
import org.darius.course.dtos.responses.EvaluationResponse;
import java.util.List;

public interface EvaluationService {

    EvaluationResponse create(String teacherId, CreateEvaluationRequest request);

    EvaluationResponse update(Long id, String teacherId, CreateEvaluationRequest request);

    void delete(Long id, String teacherId);

    EvaluationResponse getById(Long id);

    List<EvaluationResponse> getByMatiereAndSemester(Long matiereId, Long semesterId);

    /**
     * Publie les notes aux étudiants (isPublished = true).
     * Publie GradesPublishedEvent.
     */
    EvaluationResponse publish(Long id, String teacherId);

    /** Statistiques d'une évaluation (ClassStats DTO). */
    ClassStatsResponse getStats(Long evaluationId);

    /**
     * Vérifie que la somme des coefficients reste ≤ 1.0.
     * Retourne le solde disponible.
     */
    double getRemainingCoefficient(Long matiereId, Long semesterId);
}