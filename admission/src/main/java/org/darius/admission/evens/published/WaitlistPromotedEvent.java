package org.darius.admission.evens.published;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistPromotedEvent {
    private String applicationId;
    private String userId;
    private String personalEmail;
    private Long   offerId;
    private String filiereName;
    private int    rank;
    private LocalDateTime expiresAt;
}