package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.darius.course.enums.GroupType;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateStudentGroupRequest {

    @NotBlank
    private String name;

    @NotNull
    private GroupType type;

    private Long levelId;
    private Long filiereId;

    @NotNull
    private Long semesterId;

    @Min(1)
    @Builder.Default
    private int maxSize = 30;
}