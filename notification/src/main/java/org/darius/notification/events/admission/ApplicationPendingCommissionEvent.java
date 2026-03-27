package org.darius.notification.events.admission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationPendingCommissionEvent {
    private String applicationId;
    private String userId;
    private String personalEmail;
    private String firstName;
    private String academicYear;
}
