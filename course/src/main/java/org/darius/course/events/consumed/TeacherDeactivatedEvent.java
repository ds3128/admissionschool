package org.darius.course.events.consumed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeacherDeactivatedEvent {
    private String teacherId;
    private String reason;
}
