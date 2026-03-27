package org.darius.admission.common.dtos.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.darius.admission.common.enums.VoteType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CastVoteRequest {

    @NotNull(message = "Le choix est obligatoire")
    private Long choiceId;

    @NotNull(message = "Le vote est obligatoire")
    private VoteType vote;

    private String comment;
}