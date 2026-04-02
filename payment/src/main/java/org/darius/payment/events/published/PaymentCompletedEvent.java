package org.darius.payment.events.published;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentCompletedEvent {
    private String paymentReference;
    private String applicationId;    // null si pas FRAIS_DOSSIER
    private String invoiceId;        // null si pas FRAIS_SCOLARITE
    private String paymentMethod;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String type;
    private LocalDateTime paidAt;
}