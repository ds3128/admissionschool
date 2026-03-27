package org.darius.payment.events.published;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScholarshipSuspendedEvent {
    private Long scholarshipId;
    private String studentId;
    private String reason;
}
