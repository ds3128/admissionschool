package org.darius.userservice.common.dtos.responses;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StaffResponse {
    private String id;
    private String profileId;
    private String userId;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String staffNumber;
    private String position;
    private Long departmentId;
    private String departmentName;
    private boolean isActive;
    private LocalDateTime createdAt;
}