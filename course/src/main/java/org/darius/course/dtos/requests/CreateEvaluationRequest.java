package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.darius.course.enums.EvalType;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateEvaluationRequest {

    @NotBlank
    private String title;

    @NotNull
    private EvalType type;

    @NotNull
    private Long matiereId;

    @NotNull
    private Long semesterId;

    private LocalDate date;

    @DecimalMin("0.01") @DecimalMax("1.0")
    private double coefficient;

    @DecimalMin("1.0")
    @Builder.Default
    private double maxScore = 20.0;
}