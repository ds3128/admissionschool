package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateMatiereRequest {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    private String name;

    @NotNull
    private Long teachingUnitId;

    private Long departmentId;

    @DecimalMin("0.0") @DecimalMax("10.0")
    private double coefficient;

    @Min(0)
    private int hoursCM;

    @Min(0)
    private int hoursTD;

    @Min(0)
    private int hoursTP;

    @DecimalMin("0.0") @DecimalMax("100.0")
    @Builder.Default
    private double attendanceThreshold = 80.0;
}