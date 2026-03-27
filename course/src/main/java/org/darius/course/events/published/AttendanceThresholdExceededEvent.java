package org.darius.course.events.published;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceThresholdExceededEvent {
    private String studentId;
    private Long matiereId;
    private String matiereName;
    private Long semesterId;
    private double attendanceRate;
    private double threshold;
    private int totalSessions;
    private int absenceCount;
}
