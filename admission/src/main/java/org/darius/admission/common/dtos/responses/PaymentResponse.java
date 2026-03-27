package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private String id;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String paymentReference;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}