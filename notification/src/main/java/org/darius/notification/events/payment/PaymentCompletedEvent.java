package org.darius.notification.events.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentCompletedEvent {
    private String     paymentReference;
    private String     applicationId;
    private String     paymentMethod;
    private String     invoiceId;
    private String     userId;
    private BigDecimal amount;
    private String     currency;
    private String     type;
    private LocalDateTime paidAt;
}
