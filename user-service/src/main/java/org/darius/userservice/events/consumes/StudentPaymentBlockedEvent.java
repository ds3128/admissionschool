package org.darius.userservice.events.consumes;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentPaymentBlockedEvent {
    private String studentId;
    private String userId;
    private String invoiceId;
    private BigDecimal amount;
    private int overdueDays;
    private String academicYear;
}