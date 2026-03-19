package org.darius.userservice.events.produces;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentTransferredEvent {
    private String studentId;
    private String userId;
    private Long   fromFiliereId;
    private Long   toFiliereId;
    private Long   fromLevelId;
    private Long   toLevelId;
    private String reason;
}