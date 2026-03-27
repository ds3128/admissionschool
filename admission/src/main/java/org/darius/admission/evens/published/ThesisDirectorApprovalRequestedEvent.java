package org.darius.admission.evens.published;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThesisDirectorApprovalRequestedEvent {
    private Long approvalId;
    private String directorId;
    private String applicationId;
    private String candidateFirstName;
    private String candidateLastName;
    private String researchProject;
    private String expiresAt;
}