package org.darius.payment.repositories;

import org.darius.payment.entities.Payment;
import org.darius.payment.common.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByReference(String reference);

    Optional<Payment> findByExternalReference(String externalReference);

    // Vérifier si un paiement COMPLETED existe pour une candidature
    boolean existsByApplicationIdAndStatus(String applicationId, PaymentStatus status);

    // Historique des paiements d'un utilisateur
    Page<Payment> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Paiements par candidature
    List<Payment> findByApplicationIdOrderByCreatedAtDesc(String applicationId);

    // Paiements PENDING pour une candidature (pour éviter les doublons)
    Optional<Payment> findByApplicationIdAndStatus(String applicationId, PaymentStatus status);

    // Stats : total collecté par type sur une période
    @Query("""
        SELECT p.type, SUM(p.amount)
        FROM Payment p
        WHERE p.status = 'COMPLETED'
          AND (:from IS NULL OR p.paidAt >= :from)
          AND (:to IS NULL OR p.paidAt <= :to)
        GROUP BY p.type
        """)
    List<Object[]> sumCollectedByType(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to
    );
}