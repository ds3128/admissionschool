package org.darius.admission.common.dtos.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmChoiceRequest {

    @NotNull(message = "Le choix à confirmer est obligatoire")
    private Long choiceId;
}