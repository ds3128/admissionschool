package org.darius.userservice.events.produces;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherProfileCreatedEvent {
    private String teacherId;
    private String userId;
    private Long   departmentId;
    private String employeeNumber;
    private int    maxHoursPerWeek;
    // Données pour la notification
    private String firstName;
    private String lastName;
    private String personalEmail;
    private String institutionalEmail;
}