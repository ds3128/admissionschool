package org.darius.notification.events.admission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationAwaitingConfirmationEvent {
    private String             applicationId;
    private String             userId;
    private String             personalEmail;
    private String             firstName;
    private List<AcceptedChoice> acceptedChoices;
    private String             expiresAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AcceptedChoice {
        private String filiereName;
        private String level;
    }
}