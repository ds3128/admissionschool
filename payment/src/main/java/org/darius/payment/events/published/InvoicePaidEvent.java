package org.darius.payment.events.published;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoicePaidEvent {
    private String invoiceId;
    private String studentId;
    private String academicYear;
    private BigDecimal amount;
}