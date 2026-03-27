package org.darius.notification.events.course;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttendanceThresholdExceededEvent {
    private String studentId;
    private Long   matiereId;
    private String matiereName;
    private Long   semesterId;
    private double attendanceRate;
    private double threshold;
    private int    totalSessions;
    private int    absenceCount;
}
