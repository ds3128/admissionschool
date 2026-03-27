package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.CommissionType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionResponse {
    private Long id;
    private String name;
    private Long offerId;
    private String filiereName;
    private CommissionType type;
    private String presidentId;
    private int quorum;
    private List<CommissionMemberResponse> members;
    private LocalDateTime createdAt;
}