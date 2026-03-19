package org.darius.userservice.common.dtos.responses;

import lombok.*;
import org.darius.userservice.common.enums.FiliereStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FiliereResponse {
    private Long id;
    private String name;
    private String code;
    private Long departmentId;
    private String departmentName;
    private int durationYears;
    private String description;
    private FiliereStatus status;
    private List<StudyLevelResponse> studyLevels;
    private LocalDateTime createdAt;
}