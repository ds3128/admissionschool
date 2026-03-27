package org.darius.notification.events.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceGeneratedEvent {
    private String     invoiceId;
    // studentId → résolution email via User Service
    private String     studentId;
    private String     academicYear;
    private String     semester;
    private BigDecimal netAmount;
    private BigDecimal scholarshipDeduction;
    private String     dueDate;
}
