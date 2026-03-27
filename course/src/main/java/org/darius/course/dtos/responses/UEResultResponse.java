package org.darius.course.dtos.responses;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UEResultResponse {
    private Long teachingUnitId;
    private String code;
    private String name;
    private int credits;
    private double coefficient;
    private double ueAverage;
    private boolean validated;
    private List<MatiereResultResponse> matieres;
}