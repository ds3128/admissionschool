package org.darius.payment.repositories;

import org.darius.payment.entities.Invoice;
import org.darius.payment.common.enums.InvoiceStatus;
import org.darius.payment.common.enums.InvoiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    List<Invoice> findByStudentIdOrderByDueDateDesc(String studentId);

    // Unicité : éviter les doublons de génération
    boolean existsByStudentIdAndAcademicYearAndSemesterAndType(
            String studentId, String academicYear, String semester, InvoiceType type
    );

    // Factures en retard — pour le scheduler
    @Query("""
        SELECT i FROM Invoice i
        WHERE i.status IN ('PENDING', 'PARTIAL')
          AND i.dueDate < :today
        """)
    List<Invoice> findOverdueInvoices(@Param("today") LocalDate today);

    // Factures OVERDUE depuis plus de N jours — pour le blocage
    @Query("""
        SELECT i FROM Invoice i
        WHERE i.status = 'OVERDUE'
          AND i.dueDate < :threshold
        """)
    List<Invoice> findCriticalOverdueInvoices(@Param("threshold") LocalDate threshold);

    // Stats par statut
    @Query("SELECT i.status, COUNT(i) FROM Invoice i GROUP BY i.status")
    List<Object[]> countByStatus();

    // Factures dues dans N jours — pour les rappels
    @Query("""
        SELECT i FROM Invoice i
        WHERE i.status IN ('PENDING', 'PARTIAL')
          AND i.dueDate BETWEEN :today AND :reminderDate
        """)
    List<Invoice> findInvoicesDueSoon(
            @Param("today")        LocalDate today,
            @Param("reminderDate") LocalDate reminderDate
    );

    Page<Invoice> findByStatusOrderByDueDateAsc(InvoiceStatus status, Pageable pageable);
}
