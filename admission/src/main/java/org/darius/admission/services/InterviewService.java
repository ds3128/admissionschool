package org.darius.admission.services;

import org.darius.admission.common.dtos.requests.CompleteInterviewRequest;
import org.darius.admission.common.dtos.requests.ScheduleInterviewRequest;
import org.darius.admission.common.dtos.responses.InterviewResponse;

public interface InterviewService {

    /**
     * Planifie un entretien pour un choix en INTERVIEW_REQUIRED.
     * Choix → INTERVIEW_SCHEDULED.
     * Publie InterviewScheduled (Notification Service).
     */
    InterviewResponse scheduleInterview(
            String applicationId,
            Long choiceId,
            ScheduleInterviewRequest request
    );

    /** Retourne un entretien par son ID. */
    InterviewResponse getInterviewById(Long interviewId);

    /**
     * Clôture un entretien — ajoute les notes confidentielles.
     * Interview → DONE, choix → INTERVIEW_DONE.
     * Déclenche un nouveau vote de commission.
     */
    InterviewResponse completeInterview(Long interviewId, CompleteInterviewRequest request);

    /**
     * Annule un entretien.
     * Vérification : minimum 48h avant l'entretien.
     * Interview → CANCELLED, choix → INTERVIEW_REQUIRED (replanification).
     */
    InterviewResponse cancelInterview(Long interviewId, String reason);
}