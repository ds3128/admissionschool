package org.darius.admission.services;


import org.darius.admission.common.dtos.requests.CreateCampaignRequest;
import org.darius.admission.common.dtos.requests.UpdateCampaignStatusRequest;
import org.darius.admission.common.dtos.responses.CampaignResponse;
import org.darius.admission.common.dtos.responses.CampaignStatsResponse;

import java.util.List;

public interface CampaignService {

    /** Crée une campagne. Unicité de l'année académique vérifiée. */
    CampaignResponse createCampaign(CreateCampaignRequest request);

    /** Retourne toutes les campagnes. */
    List<CampaignResponse> getAllCampaigns();

    /** Retourne une campagne par son ID. */
    CampaignResponse getCampaignById(Long campaignId);

    /** Retourne la campagne actuellement OPEN. */
    CampaignResponse getCurrentCampaign();

    /** Change le statut d'une campagne (manuel). */
    CampaignResponse updateCampaignStatus(Long campaignId, UpdateCampaignStatusRequest request);

    /** Statistiques détaillées d'une campagne. */
    CampaignStatsResponse getCampaignStats(Long campaignId);

    /**
     * Job schedulé — passe les campagnes UPCOMING à OPEN si startDate <= today.
     * Et les campagnes OPEN à CLOSED si endDate < today.
     */
    void processScheduledTransitions();
}