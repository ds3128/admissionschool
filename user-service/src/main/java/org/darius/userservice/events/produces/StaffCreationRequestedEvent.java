package org.darius.userservice.events.produces;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffCreationRequestedEvent {
    private String requestId;
    private String firstName;
    private String lastName;
    private String personalEmail;
    private String role;            // ADMIN_SCHOLAR, ADMIN_RH, etc.
}