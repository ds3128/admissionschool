package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.ChoiceStatus;
import org.darius.admission.common.enums.OfferLevel;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChoiceResponse {
    private Long id;
    private Long offerId;
    private Long filiereId;
    private String filiereName;
    private OfferLevel level;
    private int choiceOrder;
    private ChoiceStatus status;
    private LocalDateTime decidedAt;
    private String decisionReason;
    private WaitlistEntryResponse waitlistEntry;
    private InterviewResponse interview;
}