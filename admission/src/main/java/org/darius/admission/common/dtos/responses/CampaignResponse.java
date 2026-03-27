package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.CampaignStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignResponse {
    private Long id;
    private String academicYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate resultsDate;
    private int confirmationDeadlineDays;
    private BigDecimal feeAmount;
    private CampaignStatus status;
    private int maxChoicesPerApplication;
    private int offerCount;
    private LocalDateTime createdAt;
}