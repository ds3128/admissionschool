package org.darius.notification.events.admission;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThesisApprovalRequestedEvent {
    private String applicationId;
    // ID de l'enseignant directeur pressenti — résolution email via User Service
    private String directorId;
    private String studentName;
    private String researchProject;
    private String expiresAt;
}
