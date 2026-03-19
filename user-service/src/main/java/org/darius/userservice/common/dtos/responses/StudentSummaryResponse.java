package org.darius.userservice.common.dtos.responses;

import lombok.*;
import org.darius.userservice.common.enums.StudentStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentSummaryResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String studentNumber;
    private String filiereName;
    private String currentLevelLabel;
    private StudentStatus status;
}