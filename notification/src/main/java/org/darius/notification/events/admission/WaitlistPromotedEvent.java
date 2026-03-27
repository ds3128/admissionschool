package org.darius.notification.events.admission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class WaitlistPromotedEvent {
    private String applicationId;
    private String userId;
    private String personalEmail;
    private String firstName;
    private String filiereName;
    private Long   offerId;
    private String expiresAt;
}
