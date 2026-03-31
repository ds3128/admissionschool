package org.darius.admission.common.dtos.responses;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminStatsResponse {
    private long totalApplications;
    private long draft;
    private long submitted;
    private long pendingAdminReview;
    private long pendingCommission;
    private long awaitingConfirmation;
    private long confirmed;
    private long rejected;
    private long expired;
    private double acceptanceRate;        // % confirmés / soumis
    private List<OfferStatLine> byOffer;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OfferStatLine {
        private Long offerId;
        private String filiereName;
        private String level;
        private int maxCapacity;
        private long confirmed;
        private long waitlisted;
        private double fillRate;          // confirmed / maxCapacity * 100
    }
}

