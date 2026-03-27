package org.darius.payment.events.published;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentFailedEvent {
    private String paymentReference;
    private String applicationId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String failureReason;
}