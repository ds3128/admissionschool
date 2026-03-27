package org.darius.course.events.published;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentEnrolledEvent {
    private String studentId;
    private Long semesterId;
    private String semesterLabel;
    private String academicYear;
    private List<Long> matiereIds;
    private List<String> matiereNames;
}
