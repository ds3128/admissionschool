package org.darius.admission.common.dtos.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReviewRequest {

    @NotNull(message = "La décision est obligatoire")
    private boolean approved;  // true = PENDING_COMMISSION, false = ADDITIONAL_DOCS_REQUIRED

    private String comment;
}