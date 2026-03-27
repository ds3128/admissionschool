package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.ApprovalStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThesisApprovalResponse {
    private Long id;
    private Long choiceId;
    private String directorId;
    private String researchProject;
    private ApprovalStatus status;
    private String comment;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime expiresAt;
}