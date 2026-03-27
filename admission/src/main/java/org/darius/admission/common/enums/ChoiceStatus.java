package org.darius.admission.common.enums;

public enum ChoiceStatus {
    PENDING_ADMIN,              // En attente validation administrative
    PENDING_COMMISSION,         // Transmis à la commission
    UNDER_COMMISSION_REVIEW,    // Commission délibère
    PENDING_THESIS_DIRECTOR,    // En attente accord directeur
    INTERVIEW_REQUIRED,         // Entretien requis
    INTERVIEW_SCHEDULED,        // Entretien planifié
    INTERVIEW_DONE,             // Entretien réalisé
    WAITLISTED,                 // Liste d'attente
    PROMOTED_FROM_WAITLIST,     // Promu — délai 48h
    ACCEPTED,                   // Accepté
    REJECTED,                   // Refusé
    CONFIRMED,                  // Confirmé par le candidat
    WITHDRAWN                   // Retiré automatiquement
}