package org.darius.admission.evens.published;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewScheduledEvent {
    private String applicationId;
    private String userId;
    private String personalEmail;
    private Long interviewId;
    private Long choiceId;
    private String filiereName;
    private LocalDateTime scheduledAt;
    private int duration;
    private String location;
    private String type;   // PRESENTIEL ou VISIO
}