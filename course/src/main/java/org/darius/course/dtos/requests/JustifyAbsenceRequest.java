package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JustifyAbsenceRequest {

    @NotBlank(message = "Le motif de justification est obligatoire")
    private String justification;
}