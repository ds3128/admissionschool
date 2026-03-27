package org.darius.course.events.consumed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeacherProfileCreatedEvent {
    private String teacherId;
    private String userId;
    private Long departmentId;
    private String firstName;
    private String lastName;
    private int maxHoursPerWeek;
}
