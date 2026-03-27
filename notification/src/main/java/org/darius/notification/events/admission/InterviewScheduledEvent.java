package org.darius.notification.events.admission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewScheduledEvent {
    private String       applicationId;
    private String       userId;
    private String       personalEmail;
    private String       firstName;
    private String       scheduledAt;
    private int          duration;      // minutes
    private String       location;
    // PRESENTIEL ou VISIO
    private String       type;
    private List<String> interviewers;
}