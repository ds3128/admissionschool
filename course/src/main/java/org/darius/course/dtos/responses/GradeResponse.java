package org.darius.course.dtos.responses;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GradeResponse {
    private Long id;
    private String studentId;
    private Long evaluationId;
    private String evaluationTitle;
    private Long matiereId;
    private String matiereName;
    private double score;
    private double maxScore;
    private double scoreOn20;
    private double coefficient;
    private String comment;
    private String gradedBy;
    private LocalDateTime gradedAt;
}