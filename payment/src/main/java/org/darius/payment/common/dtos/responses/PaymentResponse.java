package org.darius.payment.common.dtos.responses;

import lombok.*;
import org.darius.payment.common.enums.PaymentMethod;
import org.darius.payment.common.enums.PaymentStatus;
import org.darius.payment.common.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentResponse {
    private String id;
    private String userId;
    private String applicationId;
    private String invoiceId;
    private BigDecimal amount;
    private String currency;
    private PaymentType type;
    private PaymentStatus status;
    private PaymentMethod method;
    private String reference;
    private String externalReference;
    private String description;
    private String failureReason;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
