package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RescheduleSessionRequest {

    @NotNull
    private LocalDate newDate;

    @NotNull
    private LocalTime newStartTime;

    @NotNull
    private LocalTime newEndTime;

    private Long newRoomId;

    @NotBlank
    private String reason;
}