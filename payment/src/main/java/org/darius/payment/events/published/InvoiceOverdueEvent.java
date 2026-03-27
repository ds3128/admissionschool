package org.darius.payment.events.published;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceOverdueEvent {
    private String invoiceId;
    private String studentId;
    private BigDecimal remainingAmount;
    private LocalDate dueDate;
    private String academicYear;
}