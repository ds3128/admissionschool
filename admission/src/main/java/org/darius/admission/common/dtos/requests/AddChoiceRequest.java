package org.darius.admission.common.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddChoiceRequest {

    @NotNull(message = "L'offre est obligatoire")
    private Long offerId;

    @Min(value = 1, message = "L'ordre doit être entre 1 et 3")
    @Max(value = 3, message = "L'ordre doit être entre 1 et 3")
    private int choiceOrder;
}