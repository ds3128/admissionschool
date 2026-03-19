package org.darius.userservice.common.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignDepartmentHeadRequest {

    @NotBlank(message = "L'ID de l'enseignant est obligatoire")
    private String teacherId;
}