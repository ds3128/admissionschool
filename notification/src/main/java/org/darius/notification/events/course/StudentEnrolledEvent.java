package org.darius.notification.events.course;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentEnrolledEvent {
    private String       studentId;
    private Long         semesterId;
    private String       semesterLabel;
    private String       academicYear;
    private List<Long>   matiereIds;
    private List<String> matiereNames;
}
