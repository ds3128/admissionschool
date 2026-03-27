package org.darius.admission.services;

public interface ApplicationEvaluationService {

    /**
     * Évalue globalement une candidature après qu'un choix a reçu une décision finale.
     * Appelé automatiquement après chaque décision de commission.
     *
     * Logique :
     * - Si des choix sont encore en cours → ne rien faire
     * - Si au moins 1 ACCEPTED → créer ConfirmationRequest, application → AWAITING_CONFIRMATION
     * - Si tous REJECTED → application → REJECTED, publier ApplicationRejected
     * - Si certains WAITLISTED → attendre promotion
     */
    void evaluateApplication(String applicationId);

    /**
     * Job schedulé — confirmation automatique des candidatures
     * dont le délai de confirmation a expiré.
     * Confirme le choix ACCEPTED avec le plus petit choiceOrder.
     * Publie ApplicationAccepted avec autoConfirmed = true.
     */
    void processExpiredConfirmations();
}