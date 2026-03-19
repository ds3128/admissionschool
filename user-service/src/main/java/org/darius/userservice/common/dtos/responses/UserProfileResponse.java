package org.darius.userservice.common.dtos.responses;

import lombok.*;
import org.darius.userservice.common.enums.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileResponse {
    private String id;
    private String userId;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate birthDate;
    private String birthPlace;
    private String nationality;
    private Gender gender;
    private String avatarUrl;
    private String personalEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}