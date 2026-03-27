package org.darius.admission.evens.consumed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentCompletedEvent {
    private String paymentReference;  // référence pour retrouver l'AdmissionPayment
    private String applicationId;
    private BigDecimal amount;
    private String currency;
}
