package org.darius.admission.repositories;

import org.darius.admission.common.enums.ConfirmationStatus;
import org.darius.admission.entities.ConfirmationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfirmationRequestRepository
        extends JpaRepository<ConfirmationRequest, Long> {

    Optional<ConfirmationRequest> findByApplication_Id(String applicationId);

    Optional<ConfirmationRequest> findByApplication_IdAndStatus(
            String applicationId, ConfirmationStatus status
    );
}