package org.darius.course.events.consumed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentPromotedEvent {
    private String studentId;
    private Long previousLevelId;
    private Long newLevelId;
    private Long filiereId;
    private String academicYear;
}