package org.darius.admission.services;

import org.darius.admission.common.dtos.requests.AdminReviewRequest;
import org.darius.admission.common.dtos.requests.RequestAdditionalDocsRequest;
import org.darius.admission.common.dtos.responses.ApplicationResponse;
import org.darius.admission.common.dtos.responses.ApplicationSummaryResponse;
import org.darius.admission.common.dtos.responses.PageResponse;
import org.darius.admission.common.enums.ApplicationStatus;

public interface AdminApplicationService {

    /** Liste paginée de toutes les candidatures avec filtres. */
    PageResponse<ApplicationSummaryResponse> getAllApplications(
            ApplicationStatus status,
            Long campaignId,
            int page,
            int size
    );

    /** Dossier complet d'un candidat (vue admin — tous les champs). */
    ApplicationResponse getApplicationByIdAdmin(String applicationId);

    /**
     * Prise en charge administrative.
     * Si approved → PENDING_COMMISSION, choix transmis à la commission.
     * Si !approved → ADDITIONAL_DOCS_REQUIRED, dossier déverrouillé.
     */
    ApplicationResponse processAdminReview(
            String applicationId,
            String adminUserId,
            AdminReviewRequest request
    );

    /**
     * Demande de documents supplémentaires.
     * Déverrouille le dossier avec un motif.
     */
    ApplicationResponse requestAdditionalDocuments(
            String applicationId,
            String adminUserId,
            RequestAdditionalDocsRequest request
    );

    /**
     * Transmet manuellement un dossier à la commission.
     * Utilisé si la transmission automatique a échoué.
     */
    ApplicationResponse forwardToCommission(String applicationId, String adminUserId);
}