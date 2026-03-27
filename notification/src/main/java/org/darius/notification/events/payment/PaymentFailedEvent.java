package org.darius.notification.events.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentFailedEvent {
    private String     paymentReference;
    private String     applicationId;
    private String     userId;
    private BigDecimal amount;
    private String     currency;
    private String     failureReason;
}
