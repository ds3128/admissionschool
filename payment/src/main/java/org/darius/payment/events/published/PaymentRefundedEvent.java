package org.darius.payment.events.published;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentRefundedEvent {
    private String originalPaymentReference;
    private String refundPaymentReference;
    private String userId;
    private BigDecimal amount;
    private String reason;
}