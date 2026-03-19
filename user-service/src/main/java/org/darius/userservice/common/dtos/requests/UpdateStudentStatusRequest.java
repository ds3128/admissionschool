package org.darius.userservice.common.dtos.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.darius.userservice.common.enums.StudentStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStudentStatusRequest {

    @NotNull(message = "Le statut est obligatoire")
    private StudentStatus status;

    @Size(max = 500)
    private String reason;
}