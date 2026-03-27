package org.darius.course.dtos.responses;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatiereResultResponse {
    private Long matiereId;
    private String code;
    private String name;
    private double coefficient;
    private double finalScore;
    private List<GradeResponse> grades;
}
