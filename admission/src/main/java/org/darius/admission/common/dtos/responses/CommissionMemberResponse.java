package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.MemberRole;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionMemberResponse {
    private Long id;
    private String teacherId;
    private MemberRole role;
    private LocalDateTime joinedAt;
}