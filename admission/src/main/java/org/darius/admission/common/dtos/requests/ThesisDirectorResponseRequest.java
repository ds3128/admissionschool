package org.darius.admission.common.dtos.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThesisDirectorResponseRequest {

    @NotNull(message = "La décision est obligatoire")
    private boolean approved;

    private String comment;
}