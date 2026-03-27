package org.darius.admission.evens.published;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationRejectedEvent {
    private String applicationId;
    private String userId;
    private String personalEmail;
    private String candidateFirstName;
    private String candidateLastName;
    private String academicYear;
}