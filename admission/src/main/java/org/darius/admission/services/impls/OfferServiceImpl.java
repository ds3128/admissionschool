package org.darius.admission.services.impls;

import org.darius.admission.common.dtos.requests.AddRequiredDocumentRequest;
import org.darius.admission.common.dtos.requests.CreateOfferRequest;
import org.darius.admission.common.dtos.requests.UpdateOfferRequest;
import org.darius.admission.common.dtos.responses.OfferResponse;
import org.darius.admission.common.dtos.responses.OfferSummaryResponse;
import org.darius.admission.common.dtos.responses.RequiredDocumentResponse;
import org.darius.admission.common.enums.CommissionType;
import org.darius.admission.common.enums.OfferLevel;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.admission.common.enums.OfferStatus;
import org.darius.admission.entities.AdmissionCampaign;
import org.darius.admission.entities.AdmissionOffer;
import org.darius.admission.entities.RequiredDocument;
import org.darius.admission.entities.ReviewCommission;
import org.darius.admission.exceptions.InvalidOperationException;
import org.darius.admission.exceptions.ResourceNotFoundException;
import org.darius.admission.mappers.AdmissionMapper;
import org.darius.admission.repositories.AdmissionCampaignRepository;
import org.darius.admission.repositories.AdmissionOfferRepository;
import org.darius.admission.repositories.RequiredDocumentRepository;
import org.darius.admission.repositories.ReviewCommissionRepository;
import org.darius.admission.services.OfferService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferServiceImpl implements OfferService {

    private final AdmissionOfferRepository offerRepository;
    private final AdmissionCampaignRepository campaignRepository;
    private final RequiredDocumentRepository requiredDocRepository;
    private final ReviewCommissionRepository commissionRepository;
    private final AdmissionMapper mapper;

    @Override
    @Transactional
    public OfferResponse createOffer(CreateOfferRequest request) {
        AdmissionCampaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campagne introuvable : id=" + request.getCampaignId()
                ));

        // Vérification deadline dans la fenêtre de la campagne
        if (request.getDeadline().isBefore(campaign.getStartDate())
                || request.getDeadline().isAfter(campaign.getEndDate())) {
            throw new InvalidOperationException(
                    "La deadline doit être dans la fenêtre de la campagne (" +
                            campaign.getStartDate() + " → " + campaign.getEndDate() + ")"
            );
        }

        AdmissionOffer offer = AdmissionOffer.builder()
                .campaign(campaign)
                .filiereId(request.getFiliereId())
                .filiereName(request.getFiliereName())
                .level(request.getLevel())
                .deadline(request.getDeadline())
                .maxCapacity(request.getMaxCapacity())
                .status(OfferStatus.OPEN)
                .build();

        offer = offerRepository.save(offer);

        // Créer automatiquement la commission associée
        CommissionType commissionType = switch (request.getLevel()) {
            case LICENCE  -> CommissionType.LICENCE;
            case MASTER   -> CommissionType.MASTER;
            case DOCTORAT -> CommissionType.DOCTORAT;
        };

        ReviewCommission commission = ReviewCommission.builder()
                .name("Commission " + request.getFiliereName())
                .offer(offer)
                .type(commissionType)
                .quorum(3)
                .build();

        commissionRepository.save(commission);
        log.info("Offre créée : filiereId={}, level={}", offer.getFiliereId(), offer.getLevel());

        return mapper.toOfferResponse(offer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfferSummaryResponse> getOffersByCampaign(Long campaignId, OfferLevel level) {
        List<AdmissionOffer> offers = (level != null)
                ? offerRepository.findByCampaign_IdAndStatusAndLevel(campaignId, OfferStatus.OPEN, level)
                : offerRepository.findByCampaign_Id(campaignId);

        return offers.stream().map(mapper::toOfferSummaryResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OfferResponse getOfferById(Long offerId) {
        return mapper.toOfferResponse(findOfferOrThrow(offerId));
    }

    @Override
    @Transactional
    public OfferResponse updateOffer(Long offerId, UpdateOfferRequest request) {
        AdmissionOffer offer = findOfferOrThrow(offerId);

        if (request.getDeadline() != null) offer.setDeadline(request.getDeadline());
        if (request.getMaxCapacity() != null) offer.setMaxCapacity(request.getMaxCapacity());

        return mapper.toOfferResponse(offerRepository.save(offer));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequiredDocumentResponse> getRequiredDocuments(Long offerId) {
        return requiredDocRepository.findByOffer_Id(offerId).stream()
                .map(mapper::toRequiredDocumentResponse)
                .toList();
    }

    @Override
    @Transactional
    public RequiredDocumentResponse addRequiredDocument(AddRequiredDocumentRequest request) {
        AdmissionOffer offer = findOfferOrThrow(request.getOfferId());

        RequiredDocument doc = RequiredDocument.builder()
                .offer(offer)
                .documentType(request.getDocumentType())
                .label(request.getLabel())
                .description(request.getDescription())
                .isMandatory(request.isMandatory())
                .maxFileSizeMb(request.getMaxFileSizeMb())
                .build();

        return mapper.toRequiredDocumentResponse(requiredDocRepository.save(doc));
    }

    @Override
    @Transactional
    public void removeRequiredDocument(Long requiredDocId) {
        requiredDocRepository.findById(requiredDocId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document requis introuvable : id=" + requiredDocId
                ));
        requiredDocRepository.deleteById(requiredDocId);
    }

    @Override
    @Transactional
    public void processOfferStatusTransitions() {
        LocalDate today = LocalDate.now();

        // OPEN → CLOSED si deadline dépassée
        offerRepository.findExpiredOpenOffers(today).forEach(o -> {
            o.setStatus(OfferStatus.CLOSED);
            offerRepository.save(o);
            log.info("Offre {} passée à CLOSED (deadline dépassée)", o.getId());
        });

        // OPEN → FULL si acceptedCount >= maxCapacity
        offerRepository.findFullOffers().forEach(o -> {
            o.setStatus(OfferStatus.FULL);
            offerRepository.save(o);
            log.info("Offre {} passée à FULL", o.getId());
        });
    }

    private AdmissionOffer findOfferOrThrow(Long id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable : id=" + id));
    }
}
