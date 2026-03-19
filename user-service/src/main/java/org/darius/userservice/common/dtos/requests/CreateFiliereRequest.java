package org.darius.userservice.common.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateFiliereRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Le code est obligatoire")
    @Pattern(regexp = "^[A-Z0-9_]{2,20}$", message = "Le code doit être en majuscules (ex: LINF, MFIN)")
    private String code;

    @NotNull(message = "Le département est obligatoire")
    private Long departmentId;

    @Min(value = 1, message = "La durée minimale est 1 an")
    @Max(value = 8, message = "La durée maximale est 8 ans")
    private int durationYears;

    @Size(max = 500)
    private String description;
}