package org.darius.course.events.published;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SessionCancelledEvent {
    private Long sessionId;
    private Long matiereId;
    private String matiereName;
    private LocalDate date;
    private LocalTime startTime;
    private String reason;
    // IDs des étudiants du groupe concerné
    private List<String> affectedStudentIds;
}
