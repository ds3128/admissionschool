package org.darius.userservice.common.dtos.responses;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudyLevelResponse {
    private Long id;
    private String label;
    private String code;
    private int order;
    private Long filiereId;
    private String academicYear;
}