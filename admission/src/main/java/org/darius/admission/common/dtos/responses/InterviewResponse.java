package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.InterviewStatus;
import org.darius.admission.common.enums.InterviewType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewResponse {
    private Long id;
    private LocalDateTime scheduledAt;
    private int duration;
    private String location;
    private InterviewType type;
    private InterviewStatus status;
    private List<String> interviewerIds;
    // Notes non incluses — confidentielles
    private LocalDateTime createdAt;
}