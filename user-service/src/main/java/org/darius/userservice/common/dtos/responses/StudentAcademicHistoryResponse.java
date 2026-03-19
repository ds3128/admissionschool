package org.darius.userservice.common.dtos.responses;

import lombok.*;
import org.darius.userservice.common.enums.HistoryChangeReason;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentAcademicHistoryResponse {
    private Long id;
    private Long filiereId;
    private String filiereName;
    private Long levelId;
    private String levelLabel;
    private String academicYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private HistoryChangeReason reason;
    private LocalDateTime createdAt;
}