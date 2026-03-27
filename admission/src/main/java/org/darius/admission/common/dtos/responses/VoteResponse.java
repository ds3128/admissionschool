package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.VoteType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteResponse {
    private Long id;
    private Long choiceId;
    private String memberId;
    private VoteType vote;
    private String comment;
    private LocalDateTime votedAt;
}