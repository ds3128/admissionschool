package org.darius.userservice.events.consumes;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivatedEvent {
    private String userId;
    private String email;
    private String role;        // CANDIDATE, STUDENT, TEACHER, STAFF...
}