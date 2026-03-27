package org.darius.course.dtos.responses;

import lombok.*;
import org.darius.course.enums.TeachingRole;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherAssignmentResponse {
    private Long id;
    private String teacherId;
    private Long matiereId;
    private String matiereName;
    private TeachingRole role;
    private Long semesterId;
    private String semesterLabel;
    private int assignedHours;
}