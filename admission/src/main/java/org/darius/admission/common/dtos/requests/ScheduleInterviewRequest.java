package org.darius.admission.common.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.darius.admission.common.enums.InterviewType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleInterviewRequest {

    @NotNull(message = "La date est obligatoire")
    @Future(message = "La date doit être dans le futur")
    private LocalDateTime scheduledAt;

    @Min(value = 15, message = "Durée minimale : 15 minutes")
    @Builder.Default
    private int duration = 30;

    @NotBlank(message = "Le lieu est obligatoire")
    private String location;

    @NotNull(message = "Le type est obligatoire")
    private InterviewType type;

    @NotEmpty(message = "Au moins un interviewer est requis")
    private List<String> interviewerIds;
}