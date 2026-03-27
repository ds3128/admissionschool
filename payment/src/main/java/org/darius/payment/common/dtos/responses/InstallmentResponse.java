package org.darius.payment.common.dtos.responses;

import lombok.*;
import org.darius.payment.common.enums.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InstallmentResponse {
    private Long id;
    private int installmentNumber;
    private BigDecimal amount;
    private LocalDate dueDate;
    private InstallmentStatus status;
    private LocalDateTime paidAt;
}
