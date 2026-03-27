package org.darius.admission.services;

public interface StudentNumberService {

    /**
     * Génère un numéro matricule unique au format STU-{année}-{séquence 5 chiffres}.
     * Exemple : STU-2026-00042
     * Threadsafe - séquence atomique.
     */
    String generateStudentNumber();
}