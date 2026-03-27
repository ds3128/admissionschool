package org.darius.admission.services;

import org.darius.admission.common.dtos.requests.AddRequiredDocumentRequest;
import org.darius.admission.common.dtos.requests.CreateOfferRequest;
import org.darius.admission.common.dtos.requests.UpdateOfferRequest;
import org.darius.admission.common.dtos.responses.OfferResponse;
import org.darius.admission.common.dtos.responses.OfferSummaryResponse;
import org.darius.admission.common.dtos.responses.RequiredDocumentResponse;
import org.darius.admission.common.enums.OfferLevel;

import java.util.List;

public interface OfferService {

    /**
     * Crée une offre de formation.
     * Vérifie que la filière existe dans le User Service.
     * Crée automatiquement la ReviewCommission associée.
     */
    OfferResponse createOffer(CreateOfferRequest request);

    /** Retourne toutes les offres d'une campagne. */
    List<OfferSummaryResponse> getOffersByCampaign(Long campaignId, OfferLevel level);

    /** Retourne une offre par son ID avec les documents requis. */
    OfferResponse getOfferById(Long offerId);

    /** Met à jour deadline et/ou capacité d'une offre. */
    OfferResponse updateOffer(Long offerId, UpdateOfferRequest request);

    /** Retourne les documents requis pour une offre. */
    List<RequiredDocumentResponse> getRequiredDocuments(Long offerId);

    /** Ajoute un type de document requis pour une offre. */
    RequiredDocumentResponse addRequiredDocument(AddRequiredDocumentRequest request);

    /** Supprime un document requis. */
    void removeRequiredDocument(Long requiredDocId);

    /**
     * Job schedulé — passe les offres dont la deadline est dépassée à CLOSED.
     * Et les offres pleines à FULL.
     */
    void processOfferStatusTransitions();
}