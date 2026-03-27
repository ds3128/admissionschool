package org.darius.course.dtos.responses;

import lombok.*;
import org.darius.course.enums.EnrollStatus;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseDashboardEntry {
    private Long matiereId;
    private String matiereName;
    private String teachingUnitName;
    private double coefficient;
    // Moyenne provisoire — null si aucune note publiée
    private Double currentAverage;
    private double attendanceRate;
    private EnrollStatus enrollStatus;
    // Évaluations à venir
    private List<EvaluationResponse> upcomingEvaluations;
    // 3 supports récents publiés
    private int resourceCount;
    private List<CourseResourceResponse> recentResources;
}