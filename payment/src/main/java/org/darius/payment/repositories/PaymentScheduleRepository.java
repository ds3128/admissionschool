package org.darius.payment.repositories;

import org.darius.payment.entities.PaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {

    Optional<PaymentSchedule> findByInvoice_Id(String invoiceId);

    boolean existsByInvoice_Id(String invoiceId);
}