package org.darius.notification.events.admission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationAcceptedEvent {
    private String  applicationId;
    private String  userId;
    private String  studentNumber;
    private String  personalEmail;
    private String  firstName;
    private String  lastName;
    private Long    filiereId;
    private String  filiereName;
    private String  academicYear;
    private boolean autoConfirmed;
}
