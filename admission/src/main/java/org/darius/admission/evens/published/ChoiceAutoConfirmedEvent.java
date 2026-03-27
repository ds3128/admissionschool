package org.darius.admission.evens.published;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChoiceAutoConfirmedEvent {
    private String applicationId;
    private String userId;
    private String personalEmail;
    private Long   confirmedChoiceId;
    private String filiereName;
}