package org.darius.authservice.common.dtos;

import lombok.*;
import org.darius.authservice.entities.Role;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDtoResponse {
    private String id;
    private String email;
    private String role;
    private boolean status;
    private LocalDateTime lastLogin;
}
