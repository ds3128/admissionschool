package org.darius.admission.evens.published;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationAwaitingConfirmationEvent {
    private String applicationId;
    private String userId;
    private String personalEmail;
    private String candidateFirstName;
    private String candidateLastName;
    private List<AcceptedChoiceSummary> acceptedChoices;
    private LocalDateTime expiresAt;

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AcceptedChoiceSummary {
        private Long choiceId;
        private String filiereName;
        private String level;
        private int choiceOrder;
    }
}