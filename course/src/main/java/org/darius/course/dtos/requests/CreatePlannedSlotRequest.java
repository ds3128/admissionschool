package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.darius.course.enums.SessionType;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreatePlannedSlotRequest {

    @NotNull
    private Long matiereId;

    @NotBlank
    private String teacherId;

    @NotNull
    private Long roomId;

    @NotNull
    private Long groupId;

    @NotNull
    private Long semesterId;

    @NotNull
    private DayOfWeek dayOfWeek;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @NotNull
    private SessionType type;

    @Builder.Default
    private boolean recurrent = true;
}