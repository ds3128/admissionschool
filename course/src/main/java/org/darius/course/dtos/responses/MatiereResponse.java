package org.darius.course.dtos.responses;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatiereResponse {
    private Long id;
    private String code;
    private String name;
    private Long teachingUnitId;
    private String teachingUnitName;
    private Long departmentId;
    private double coefficient;
    private int totalHours;
    private int hoursCM;
    private int hoursTD;
    private int hoursTP;
    private double attendanceThreshold;
}