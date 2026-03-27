package org.darius.admission.repositories;

import org.darius.admission.common.enums.PaymentStatus;
import org.darius.admission.entities.AdmissionPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdmissionPaymentRepository extends JpaRepository<AdmissionPayment, String> {

    Optional<AdmissionPayment> findByApplication_Id(String applicationId);

    Optional<AdmissionPayment> findByApplication_IdAndStatus(
            String applicationId, PaymentStatus status
    );

    Optional<AdmissionPayment> findByPaymentReference(String paymentReference);
}