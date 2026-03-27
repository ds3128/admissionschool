package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.ApplicationStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationSummaryResponse {
    private String id;
    private String academicYear;
    private ApplicationStatus status;
    private String candidateFirstName;
    private String candidateLastName;
    private String candidateEmail;
    private int choiceCount;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}