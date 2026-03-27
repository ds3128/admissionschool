package org.darius.admission.common.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.darius.admission.common.enums.DocumentType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddRequiredDocumentRequest {

    @NotNull(message = "L'offre est obligatoire")
    private Long offerId;

    @NotNull(message = "Le type de document est obligatoire")
    private DocumentType documentType;

    @NotBlank(message = "Le libellé est obligatoire")
    private String label;

    private String description;

    @Builder.Default
    private boolean isMandatory = true;

    @Min(value = 1) @Max(value = 20)
    @Builder.Default
    private int maxFileSizeMb = 5;
}