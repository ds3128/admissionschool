package org.darius.authservice.events;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UserActivatedEvent {
    private String userId;
    private String email;
    private String role;
}