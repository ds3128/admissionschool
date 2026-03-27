package org.darius.payment.repositories;

import org.darius.payment.entities.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    List<Installment> findBySchedule_IdOrderByInstallmentNumber(Long scheduleId);

    // Prochaine échéance non payée
    @Query("""
        SELECT i FROM Installment i
        WHERE i.schedule.id = :scheduleId
          AND i.status = 'PENDING'
        ORDER BY i.installmentNumber
        LIMIT 1
        """)
    Optional<Installment> findNextPendingInstallment(@Param("scheduleId") Long scheduleId);

    // Échéances en retard — pour le scheduler
    @Query("""
        SELECT i FROM Installment i
        WHERE i.status = 'PENDING'
          AND i.dueDate < :today
        """)
    List<Installment> findOverdueInstallments(@Param("today") LocalDate today);
}
