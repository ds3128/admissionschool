package org.darius.course.services;

import org.darius.course.dtos.requests.CreateAttachmentRequest;
import org.darius.course.dtos.responses.EvaluationAttachmentResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface EvaluationAttachmentService {

    /**
     * Ajoute une pièce jointe à une évaluation.
     * Visible immédiatement — pas de brouillon.
     * L'enseignant doit être affecté à la matière de l'évaluation.
     */
    EvaluationAttachmentResponse addAttachment(
            Long evaluationId,
            String teacherId,
            CreateAttachmentRequest request,
            MultipartFile file
    );

    /**
     * Soft delete — isDeleted = true.
     * Seul l'auteur peut supprimer.
     */
    void delete(Long attachmentId, String teacherId);

    /** Liste des pièces jointes visibles d'une évaluation. */
    List<EvaluationAttachmentResponse> getByEvaluation(Long evaluationId);
}