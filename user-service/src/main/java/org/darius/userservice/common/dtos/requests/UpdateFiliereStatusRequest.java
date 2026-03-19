package org.darius.userservice.common.dtos.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.darius.userservice.common.enums.FiliereStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFiliereStatusRequest {

    @NotNull(message = "Le statut est obligatoire")
    private FiliereStatus status;
}