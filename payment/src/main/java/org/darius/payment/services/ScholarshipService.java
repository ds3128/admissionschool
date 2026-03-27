package org.darius.payment.services;

import org.darius.payment.common.dtos.requests.*;
import org.darius.payment.common.dtos.responses.*;
import org.darius.payment.events.consumed.SemesterValidatedEvent;

import java.util.List;

public interface ScholarshipService {

    /** Attribue une bourse à un étudiant. */
    ScholarshipResponse createScholarship(String adminId, CreateScholarshipRequest request);

    /** Active une bourse et génère les ScholarshipDisbursement. */
    ScholarshipResponse activateScholarship(Long scholarshipId, String adminId);

    /** Suspend une bourse — annule les versements futurs. */
    ScholarshipResponse suspendScholarship(Long scholarshipId, String adminId, SuspendScholarshipRequest request);

    /** Termine définitivement une bourse. */
    ScholarshipResponse terminateScholarship(Long scholarshipId, String adminId);

    /** Ma bourse (étudiant connecté). */
    List<ScholarshipResponse> getMyScholarships(String studentId);

    /** Détail d'une bourse. */
    ScholarshipResponse getScholarshipById(Long scholarshipId);

    /** Liste des bourses avec filtres — admin. */
    List<ScholarshipResponse> getScholarships(String studentId, String type, String status);

    /** Historique des versements d'une bourse. */
    List<DisbursementResponse> getDisbursements(Long scholarshipId);

    /** Job schedulé — traite les versements du mois. */
    void processDisbursements();

    /** Job schedulé — renouvellement annuel des bourses mérite (1er juillet). */
    void processAnnualMeritRenewal(String academicYear);

    /** Déclenché par l'event SemesterValidated (Kafka). */
    void processMeritRenewalFromEvent(SemesterValidatedEvent event);
}