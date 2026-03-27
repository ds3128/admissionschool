package org.darius.payment.events.published;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScholarshipActivatedEvent {
    private Long scholarshipId;
    private String studentId;
    private String type;
    private BigDecimal amount;
    private BigDecimal disbursementAmount;
    private String frequency;
    private String academicYear;
}