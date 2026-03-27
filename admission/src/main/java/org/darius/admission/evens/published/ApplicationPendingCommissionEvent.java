package org.darius.admission.evens.published;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationPendingCommissionEvent {
    private String applicationId;
    private String userId;
    private String personalEmail;
    private Long offerId;
    private String filiereName;
}