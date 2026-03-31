package org.darius.payment.common.dtos.responses;

import lombok.*;

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
    private String status;
}