package org.darius.userservice.common.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.darius.userservice.common.enums.AcademicGrade;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTeacherRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "L'email personnel est obligatoire")
    @Email(message = "Format d'email invalide")
    private String personalEmail;

    @Size(max = 150)
    private String speciality;

    @NotNull(message = "Le grade académique est obligatoire")
    private AcademicGrade grade;

    @NotNull(message = "Le département est obligatoire")
    private Long departmentId;

    @Size(max = 150)
    private String diploma;

    @Min(value = 1, message = "Le volume horaire minimum est 1h")
    @Max(value = 40, message = "Le volume horaire maximum est 40h")
    private int maxHoursPerWeek;
}