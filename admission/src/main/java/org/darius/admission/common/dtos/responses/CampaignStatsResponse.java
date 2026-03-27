package org.darius.admission.common.dtos.responses;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignStatsResponse {
    private Long campaignId;
    private String academicYear;
    private int totalApplications;
    private int draftCount;
    private int submittedCount;
    private int underReviewCount;
    private int acceptedCount;
    private int rejectedCount;
    private int waitlistedCount;
    private int awaitingConfirmationCount;
    // Statistiques par offre : offerId → nombre d'inscrits
    private Map<Long, Integer> acceptedByOffer;
}