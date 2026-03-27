package org.darius.course.events.consumed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentProfileCreatedEvent {
    private String studentId;
    private String userId;
    private Long filiereId;
    private Long levelId;
    private String firstName;
    private String lastName;
    private String academicYear;
}