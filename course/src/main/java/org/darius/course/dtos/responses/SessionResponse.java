package org.darius.course.dtos.responses;

import lombok.*;
import org.darius.course.enums.SessionStatus;
import org.darius.course.enums.SessionType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SessionResponse {
    private Long id;
    private Long plannedSlotId;
    private String teacherId;
    private Long matiereId;
    private String matiereName;
    private Long roomId;
    private String roomName;
    private LocalDate date;
    private LocalTime startTime;
    private int duration;
    private SessionType type;
    private SessionStatus status;
    private String cancelReason;
    private LocalDateTime createdAt;
}