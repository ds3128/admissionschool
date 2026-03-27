package org.darius.payment.common.enums;

public enum ScholarshipSource {
    INSTITUTIONNELLE,  // versée par l'université — génère des ScholarshipDisbursement
    GOUVERNEMENTALE,   // OSAP, bourse fédérale — déduction sur facture uniquement
    EXTERNE            // fondation, entreprise — déduction sur facture uniquement
}
