package org.darius.userservice.events.produces;

import lombok.*;
import org.darius.userservice.common.enums.StudentStatus;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentStatusChangedEvent {
    private String studentId;
    private String userId;
    private String firstName;
    private String lastName;
    private String personalEmail;
    private StudentStatus oldStatus;
    private StudentStatus newStatus;
    private String reason;
}
