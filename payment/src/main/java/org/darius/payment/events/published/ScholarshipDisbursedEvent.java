package org.darius.payment.events.published;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScholarshipDisbursedEvent {
    private Long scholarshipId;
    private String studentId;
    private BigDecimal amount;
    private String period;
    private String paymentReference;
}
