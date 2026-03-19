package org.darius.userservice.events.produces;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPromotedEvent {
    private String studentId;
    private String userId;
    private Long   filiereId;
    private Long   fromLevelId;
    private Long   toLevelId;
    private String fromLevelLabel;
    private String toLevelLabel;
    private String academicYear;
}