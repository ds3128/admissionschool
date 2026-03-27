package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.ApplicationStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusHistoryResponse {
    private Long id;
    private ApplicationStatus fromStatus;
    private ApplicationStatus toStatus;
    private String changedBy;
    private String comment;
    private LocalDateTime changedAt;
}