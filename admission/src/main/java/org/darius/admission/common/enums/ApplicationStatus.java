package org.darius.admission.common.enums;

public enum ApplicationStatus {
    DRAFT,                      // Brouillon en cours
    PAID,                       // Frais payés
    SUBMITTED,                  // Dossier soumis et verrouillé
    UNDER_ADMIN_REVIEW,         // Vérification administrative
    ADDITIONAL_DOCS_REQUIRED,   // Documents supplémentaires demandés
    PENDING_COMMISSION,         // Transmis à la commission
    UNDER_COMMISSION_REVIEW,    // Commission en cours d'examen
    PENDING_THESIS_DIRECTOR,    // En attente accord directeur (Doctorat)
    INTERVIEW_SCHEDULED,        // Entretien planifié
    INTERVIEW_DONE,             // Entretien réalisé
    AWAITING_CONFIRMATION,      // Au moins 1 choix accepté
    ACCEPTED,                   // Confirmation effectuée
    REJECTED,                   // Tous les choix refusés
    WITHDRAWN                   // Retirée par le candidat
}