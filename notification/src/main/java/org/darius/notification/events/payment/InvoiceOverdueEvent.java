package org.darius.notification.events.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceOverdueEvent {
    private String     invoiceId;
    private String     studentId;
    private BigDecimal remainingAmount;
    private String     dueDate;
    private String     academicYear;
}