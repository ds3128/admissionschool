package org.darius.admission.services;

import org.darius.admission.common.dtos.requests.ThesisDirectorResponseRequest;
import org.darius.admission.common.dtos.responses.ThesisApprovalResponse;

import java.util.List;

public interface ThesisDirectorService {

    /**
     * Retourne les demandes d'accord en attente pour un directeur.
     */
    List<ThesisApprovalResponse> getPendingApprovals(String directorId);

    /**
     * Enregistre la réponse du directeur.
     * Si approved → choix → PENDING_COMMISSION.
     * Si !approved → choix → REJECTED, notification candidat.
     */
    ThesisApprovalResponse respondToApproval(
            Long approvalId,
            String directorId,
            ThesisDirectorResponseRequest request
    );

    /**
     * Job schedulé — expire les demandes sans réponse après 15 jours.
     * Choix → REJECTED. Publie notification.
     */
    void processExpiredApprovals();

    /**
     * Job schedulé — envoie les rappels à J+7.
     */
    void sendReminders();
}