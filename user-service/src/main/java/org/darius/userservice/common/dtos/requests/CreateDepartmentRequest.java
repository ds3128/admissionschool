package org.darius.userservice.common.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateDepartmentRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Le code est obligatoire")
    @Pattern(regexp = "^[A-Z0-9_]{2,20}$", message = "Le code doit être en majuscules (ex: INFO, MATH)")
    private String code;

    @Size(max = 500)
    private String description;
}