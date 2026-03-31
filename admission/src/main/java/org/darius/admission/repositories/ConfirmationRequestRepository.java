package org.darius.admission.repositories;

import org.darius.admission.common.enums.ConfirmationStatus;
import org.darius.admission.entities.ConfirmationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConfirmationRequestRepository
        extends JpaRepository<ConfirmationRequest, Long> {

    Optional<ConfirmationRequest> findByApplication_Id(String applicationId);

    Optional<ConfirmationRequest> findByApplication_IdAndStatus(
            String applicationId, ConfirmationStatus status
    );

    @Query("""
    SELECT cr FROM ConfirmationRequest cr
    WHERE cr.status = 'PENDING'
      AND cr.expiresAt < :now
    """)
    List<ConfirmationRequest> findExpiredPending(@Param("now") LocalDateTime now);
}