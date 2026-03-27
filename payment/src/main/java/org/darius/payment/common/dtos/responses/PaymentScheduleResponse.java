package org.darius.payment.common.dtos.responses;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentScheduleResponse {
    private Long id;
    private int totalInstallments;
    private int paidInstallments;
    private BigDecimal totalAmount;
    private List<InstallmentResponse> installments;
    private LocalDateTime createdAt;
}
