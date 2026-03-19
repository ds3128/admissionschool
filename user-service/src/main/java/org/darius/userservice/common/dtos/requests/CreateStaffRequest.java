package org.darius.userservice.common.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateStaffRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "L'email personnel est obligatoire")
    @Email(message = "Format d'email invalide")
    private String personalEmail;

    @NotBlank(message = "Le poste est obligatoire")
    @Size(max = 150)
    private String position;

    @NotNull(message = "Le département est obligatoire")
    private Long departmentId;
}