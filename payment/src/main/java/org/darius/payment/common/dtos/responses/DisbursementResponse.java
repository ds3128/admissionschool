package org.darius.payment.common.dtos.responses;

import lombok.*;
import org.darius.payment.common.enums.DisbursementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DisbursementResponse {
    private Long id;
    private BigDecimal amount;
    private String period;
    private LocalDate scheduledDate;
    private DisbursementStatus status;
    private String paymentId;
    private LocalDateTime paidAt;
    private String failureReason;
}