package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CancelSessionRequest {

    @NotBlank(message = "Le motif d'annulation est obligatoire")
    private String reason;
}