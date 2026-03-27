package org.darius.payment.common.dtos.responses;

import lombok.*;
import org.darius.payment.common.enums.InvoiceStatus;
import org.darius.payment.common.enums.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceResponse {
    private String id;
    private String studentId;
    private String academicYear;
    private String semester;
    private InvoiceType type;
    private BigDecimal amount;
    private BigDecimal scholarshipDeduction;
    private BigDecimal netAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private boolean hasSchedule;
    private String currency;
    private PaymentScheduleResponse schedule;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
