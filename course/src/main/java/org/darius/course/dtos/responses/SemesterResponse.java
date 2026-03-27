package org.darius.course.dtos.responses;

import lombok.*;
import org.darius.course.enums.SemesterStatus;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SemesterResponse {
    private Long id;
    private String label;
    private String academicYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isCurrent;
    private boolean isLastOfYear;
    private SemesterStatus status;
}