package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubmitGradesRequest {

    @NotEmpty
    private List<GradeEntry> grades;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class GradeEntry {
        @NotBlank
        private String studentId;

        @DecimalMin("0.0")
        private double score;

        private String comment;
    }
}