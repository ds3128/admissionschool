package org.darius.userservice.common.dtos.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BulkPromoteRequest {

    @NotNull(message = "La filière est obligatoire")
    private Long filiereId;

    @NotNull(message = "Le niveau source est obligatoire")
    private Long fromLevelId;

    // Si null → promeut tous les ACTIVE du niveau
    private List<String> studentIds;
}

