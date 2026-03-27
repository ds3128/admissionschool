package org.darius.payment.repositories;

import org.darius.payment.entities.ScholarshipDisbursement;
import org.darius.payment.common.enums.DisbursementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScholarshipDisbursementRepository
        extends JpaRepository<ScholarshipDisbursement, Long> {

    List<ScholarshipDisbursement> findByScholarship_IdOrderByScheduledDateDesc(Long scholarshipId);

    // Versements à traiter ce mois — pour le scheduler
    @Query("""
        SELECT d FROM ScholarshipDisbursement d
        WHERE d.status = 'SCHEDULED'
          AND d.scheduledDate <= :today
        """)
    List<ScholarshipDisbursement> findDueForProcessing(@Param("today") LocalDate today);

    // Versements futurs d'une bourse — pour annulation lors suspension
    List<ScholarshipDisbursement> findByScholarship_IdAndStatus(
            Long scholarshipId, DisbursementStatus status
    );
}