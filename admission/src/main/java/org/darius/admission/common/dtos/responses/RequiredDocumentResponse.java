package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.DocumentType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequiredDocumentResponse {
    private Long id;
    private DocumentType documentType;
    private String label;
    private String description;
    private boolean isMandatory;
    private int maxFileSizeMb;
}