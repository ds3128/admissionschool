package org.darius.notification.events.admission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationSubmittedEvent {
    private String applicationId;
    private String userId;
    private String academicYear;
    private String candidateFirstName;
    private String candidateLastName;
    private String personalEmail;
    private int choiceCount;
    private LocalDate submittedAt;
}