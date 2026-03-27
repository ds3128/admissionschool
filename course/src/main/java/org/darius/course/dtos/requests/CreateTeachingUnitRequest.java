package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateTeachingUnitRequest {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    private String name;

    @Min(1) @Max(300)
    private int credits;

    @NotNull
    private Long studyLevelId;

    @Min(1) @Max(2)
    private int semesterNumber;

    @DecimalMin("0.0") @DecimalMax("10.0")
    private double coefficient;
}