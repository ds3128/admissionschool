package org.darius.admission.services;


import org.darius.admission.common.dtos.requests.AddCommissionMemberRequest;
import org.darius.admission.common.dtos.requests.CastVoteRequest;
import org.darius.admission.common.dtos.requests.ValidateDecisionRequest;
import org.darius.admission.common.dtos.responses.ChoiceResponse;
import org.darius.admission.common.dtos.responses.CommissionResponse;
import org.darius.admission.common.dtos.responses.VoteResponse;
import org.darius.admission.common.dtos.responses.VoteResultResponse;

import java.util.List;

public interface CommissionService {

    /** Retourne toutes les commissions. */
    List<CommissionResponse> getAllCommissions();

    /** Retourne une commission par son ID. */
    CommissionResponse getCommissionById(Long commissionId);

    /** Ajoute un membre à la commission. */
    CommissionResponse addMember(Long commissionId, AddCommissionMemberRequest request);

    /** Retire un membre de la commission. */
    CommissionResponse removeMember(Long commissionId, Long memberId);

    /**
     * Retourne les choix en attente d'examen pour une commission.
     * Seuls les membres de la commission peuvent accéder à cette liste.
     */
    List<ChoiceResponse> getPendingChoicesForCommission(
            Long commissionId,
            String teacherId
    );

    /**
     * Enregistre le vote d'un membre sur un choix.
     * Vérifie que le membre appartient à la commission.
     * Vérifie qu'il n'a pas déjà voté.
     * Si tous ont voté → calcule le résultat automatiquement.
     */
    VoteResponse castVote(Long commissionId, String teacherId, CastVoteRequest request);

    /** Retourne le résultat du vote pour un choix. */
    VoteResultResponse getVoteResult(Long commissionId, Long choiceId);

    /**
     * Valide la décision finale du président.
     * Décision possible : ACCEPTED, REJECTED, WAITLISTED, INTERVIEW_REQUIRED.
     * Si WAITLISTED → crée WaitlistEntry.
     * Déclenche l'évaluation globale si tous les choix ont une décision.
     */
    ChoiceResponse validateDecision(
            Long commissionId,
            Long choiceId,
            String presidentId,
            ValidateDecisionRequest request
    );
}