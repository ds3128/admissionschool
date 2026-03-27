package org.darius.payment.common.dtos.responses;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinanceStatsResponse {
    private String academicYear;
    private BigDecimal totalCollected;
    private BigDecimal totalPending;
    private BigDecimal totalOverdue;
    private BigDecimal totalScholarships;
    private int invoicePaidCount;
    private int invoiceOverdueCount;
    private int invoicePendingCount;
    private int blockedStudentsCount;
    private int activeScholarshipsCount;
    private Map<String, BigDecimal> collectedByType;
}
