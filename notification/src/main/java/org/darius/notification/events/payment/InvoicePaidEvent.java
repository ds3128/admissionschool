package org.darius.notification.events.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoicePaidEvent {
    private String     invoiceId;
    private String     studentId;
    private String     academicYear;
    private BigDecimal amount;
}
