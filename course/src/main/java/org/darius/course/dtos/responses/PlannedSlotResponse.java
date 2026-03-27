package org.darius.course.dtos.responses;

import lombok.*;
import org.darius.course.enums.SessionType;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlannedSlotResponse {
    private Long id;
    private Long matiereId;
    private String matiereName;
    private String teacherId;
    private Long roomId;
    private String roomName;
    private Long groupId;
    private String groupName;
    private Long semesterId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private SessionType type;
    private boolean recurrent;
}
