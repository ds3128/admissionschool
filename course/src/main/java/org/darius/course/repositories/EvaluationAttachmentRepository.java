package org.darius.course.repositories;

import org.darius.course.entities.EvaluationAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationAttachmentRepository extends JpaRepository<EvaluationAttachment, Long> {

    // Pièces jointes visibles d'une évaluation (non supprimées)
    List<EvaluationAttachment> findByEvaluation_IdAndIsDeletedFalse(Long evaluationId);

    // Pièces jointes d'un enseignant pour une évaluation
    List<EvaluationAttachment> findByEvaluation_IdAndUploadedByAndIsDeletedFalse(
            Long evaluationId, String uploadedBy
    );
}