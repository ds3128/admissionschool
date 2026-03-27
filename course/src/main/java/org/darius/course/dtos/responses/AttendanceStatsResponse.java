package org.darius.course.dtos.responses;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceStatsResponse {
    private String studentId;
    private Long matiereId;
    private String matiereName;
    private Long semesterId;
    private int totalSessions;
    private int presentCount;
    private int absenceCount;
    private int justifiedCount;
    private double attendanceRate;
    private boolean blocked;
}