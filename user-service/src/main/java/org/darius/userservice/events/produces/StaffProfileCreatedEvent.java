package org.darius.userservice.events.produces;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffProfileCreatedEvent {
    private String staffId;
    private String userId;
    private Long   departmentId;
    private String staffNumber;
    // Données pour la notification
    private String firstName;
    private String lastName;
    private String personalEmail;
    private String institutionalEmail;
}