package org.darius.userservice.common.dtos;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateMinimalProfilRequest {
    private String userId;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
}
