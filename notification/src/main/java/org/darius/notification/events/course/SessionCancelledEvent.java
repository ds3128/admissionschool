package org.darius.notification.events.course;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionCancelledEvent {
    private Long         sessionId;
    private Long         matiereId;
    private String       matiereName;
    private String       date;
    private String       startTime;
    private String       reason;
    // Plusieurs destinataires → envoi bulk
    private List<String> affectedStudentIds;
}
