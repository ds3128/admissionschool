package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateSemesterRequest {

    @NotBlank
    private String label;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{4}", message = "Format attendu : 2026-2027")
    private String academicYear;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @Builder.Default
    private boolean isLastOfYear = false;
}