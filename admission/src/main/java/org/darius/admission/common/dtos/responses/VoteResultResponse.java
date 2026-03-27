package org.darius.admission.common.dtos.responses;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteResultResponse {
    private Long choiceId;
    private int totalVotes;
    private int acceptVotes;
    private int rejectVotes;
    private int abstainVotes;
    private boolean quorumReached;
    private String suggestedDecision; // ACCEPTED, REJECTED, TIE
}