package org.darius.payment.mappers;

import org.darius.payment.common.dtos.responses.*;
import org.darius.payment.entities.*;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PaymentMapper {

    PaymentResponse toPaymentResponse(Payment payment);

    @Mapping(target = "schedule", source = "schedule")
    InvoiceResponse toInvoiceResponse(Invoice invoice);

    @Mapping(target = "installments", source = "installments")
    PaymentScheduleResponse toScheduleResponse(PaymentSchedule schedule);

    InstallmentResponse toInstallmentResponse(Installment installment);

    ScholarshipResponse toScholarshipResponse(Scholarship scholarship);

    DisbursementResponse toDisbursementResponse(ScholarshipDisbursement disbursement);
}