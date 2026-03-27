package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.darius.course.enums.TeachingRole;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateTeacherAssignmentRequest {

    @NotBlank
    private String teacherId;

    @NotNull
    private Long matiereId;

    @NotNull
    private TeachingRole role;

    @NotNull
    private Long semesterId;

    @Min(1)
    private int assignedHours;
}