package org.darius.userservice.common.dtos.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.darius.userservice.common.enums.HistoryChangeReason;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransferStudentRequest {

    @NotNull(message = "La filière cible est obligatoire")
    private Long targetFiliereId;

    @NotNull(message = "Le niveau cible est obligatoire")
    private Long targetLevelId;

    @NotNull(message = "La raison est obligatoire")
    private HistoryChangeReason reason;

    @Size(max = 500)
    private String comment;
}