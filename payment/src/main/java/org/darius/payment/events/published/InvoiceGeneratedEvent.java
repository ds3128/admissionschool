package org.darius.payment.events.published;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceGeneratedEvent {
    private String invoiceId;
    private String studentId;
    private String academicYear;
    private String semester;
    private BigDecimal netAmount;
    private BigDecimal scholarshipDeduction;
    private LocalDate dueDate;
}
