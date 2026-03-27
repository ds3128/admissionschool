package org.darius.course.events.published;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GradesPublishedEvent {
    private Long evaluationId;
    private String evaluationTitle;
    private Long matiereId;
    private String matiereName;
    private Long semesterId;
    // IDs des étudiants concernés
    private List<String> studentIds;
}
