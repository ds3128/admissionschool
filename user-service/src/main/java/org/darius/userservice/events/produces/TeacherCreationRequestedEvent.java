package org.darius.userservice.events.produces;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherCreationRequestedEvent {
    private String requestId;       // pour corréler la réponse
    private String firstName;
    private String lastName;
    private String personalEmail;
    private String role;            // TEACHER
}