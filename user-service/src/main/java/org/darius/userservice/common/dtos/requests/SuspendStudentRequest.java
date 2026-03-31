package org.darius.userservice.common.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SuspendStudentRequest {

    @NotBlank(message = "La raison de suspension est obligatoire")
    @Size(max = 500)
    private String reason;

    private LocalDate suspendedUntil; // null = indefini
}
