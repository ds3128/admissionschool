package org.darius.course.dtos.responses;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassStatsResponse {
    private Long evaluationId;
    private String evaluationTitle;
    private Long matiereId;
    private String matiereName;
    private double average;
    private double min;
    private double max;
    private double passRate;
    private double standardDeviation;
    private int totalStudents;
}