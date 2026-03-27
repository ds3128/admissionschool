package org.darius.admission.evens.published;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationSubmittedEvent {
    private String applicationId;
    private String userId;
    private String academicYear;
    private String candidateFirstName;
    private String candidateLastName;
    private String personalEmail;
    private int choiceCount;
}