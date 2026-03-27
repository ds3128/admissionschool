package org.darius.notification.events.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScholarshipDisbursedEvent {
    private Long       scholarshipId;
    private String     studentId;
    private BigDecimal amount;
    private String     period;
    private String     paymentReference;
}
