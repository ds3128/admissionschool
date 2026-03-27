package org.darius.course.dtos.responses;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WeeklyScheduleResponse {
    private Long semesterId;
    private String semesterLabel;
    private int weekNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<PlannedSlotResponse> slots;
    private List<SessionResponse> sessions;
}