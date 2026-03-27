package org.darius.course.dtos.responses;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeachingUnitResponse {
    private Long id;
    private String code;
    private String name;
    private int credits;
    private Long studyLevelId;
    private int semesterNumber;
    private double coefficient;
    private List<MatiereResponse> matieres;
}
