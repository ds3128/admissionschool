package org.darius.admission.common.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestAdditionalDocsRequest {

    @NotBlank(message = "Le motif est obligatoire")
    private String reason;
}