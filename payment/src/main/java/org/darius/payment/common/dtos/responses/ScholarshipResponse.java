package org.darius.payment.common.dtos.responses;

import lombok.*;
import org.darius.payment.common.enums.DisbursementFrequency;
import org.darius.payment.common.enums.ScholarshipStatus;
import org.darius.payment.common.enums.ScholarshipType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScholarshipResponse {
    private Long id;
    private String studentId;
    private String academicYear;
    private BigDecimal amount;
    private BigDecimal disbursementAmount;
    private DisbursementFrequency disbursementFrequency;
    private ScholarshipType type;
    private ScholarshipStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String conditions;
    private BigDecimal minimumGrade;
    private String suspensionReason;
    private LocalDateTime createdAt;
}