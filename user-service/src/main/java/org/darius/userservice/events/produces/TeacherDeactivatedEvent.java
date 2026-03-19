package org.darius.userservice.events.produces;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherDeactivatedEvent {
    private String teacherId;
    private String userId;
    private Long   departmentId;
}