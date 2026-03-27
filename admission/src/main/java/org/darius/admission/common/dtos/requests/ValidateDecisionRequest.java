package org.darius.admission.common.dtos.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.darius.admission.common.enums.ChoiceStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateDecisionRequest {

    @NotNull(message = "La décision est obligatoire")
    private ChoiceStatus decision; // ACCEPTED, REJECTED, WAITLISTED, INTERVIEW_REQUIRED

    private String reason;
}