package org.darius.admission.services.impls;

import org.apache.kafka.common.errors.DuplicateResourceException;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.darius.admission.common.dtos.requests.CreateCampaignRequest;
import org.darius.admission.common.dtos.requests.UpdateCampaignStatusRequest;
import org.darius.admission.common.dtos.responses.CampaignResponse;
import org.darius.admission.common.dtos.responses.CampaignStatsResponse;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.admission.common.enums.ApplicationStatus;
import org.darius.admission.common.enums.CampaignStatus;
import org.darius.admission.entities.AdmissionCampaign;
import org.darius.admission.exceptions.InvalidOperationException;
import org.darius.admission.mappers.AdmissionMapper;
import org.darius.admission.repositories.AdmissionCampaignRepository;
import org.darius.admission.repositories.ApplicationRepository;
import org.darius.admission.services.CampaignService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignServiceImpl implements CampaignService {

    private final AdmissionCampaignRepository campaignRepository;
    private final ApplicationRepository applicationRepository;
    private final AdmissionMapper mapper;

    @Override
    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request) {
        if (campaignRepository.existsByAcademicYear(request.getAcademicYear())) {
            throw new DuplicateResourceException(
                    "Une campagne existe déjà pour l'année " + request.getAcademicYear()
            );
        }

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new InvalidOperationException(
                    "La date de clôture doit être après la date d'ouverture"
            );
        }

        AdmissionCampaign campaign = AdmissionCampaign.builder()
                .academicYear(request.getAcademicYear())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .resultsDate(request.getResultsDate())
                .confirmationDeadlineDays(request.getConfirmationDeadlineDays())
                .feeAmount(request.getFeeAmount())
                .maxChoicesPerApplication(request.getMaxChoicesPerApplication())
                .status(CampaignStatus.UPCOMING)
                .build();

        campaign = campaignRepository.save(campaign);
        log.info("Campagne créée : {} ({})", campaign.getAcademicYear(), campaign.getId());
        return mapper.toCampaignResponse(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignResponse> getAllCampaigns() {
        return campaignRepository.findAll().stream()
                .map(mapper::toCampaignResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(Long campaignId) {
        return mapper.toCampaignResponse(findOrThrow(campaignId));
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getCurrentCampaign() {
        return campaignRepository.findByStatus(CampaignStatus.OPEN)
                .map(mapper::toCampaignResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune campagne ouverte en ce moment"
                ));
    }

    @Override
    @Transactional
    public CampaignResponse updateCampaignStatus(
            Long campaignId, UpdateCampaignStatusRequest request
    ) {
        AdmissionCampaign campaign = findOrThrow(campaignId);

        if (campaign.getStatus() == CampaignStatus.ARCHIVED) {
            throw new InvalidOperationException("Une campagne archivée ne peut pas changer de statut");
        }

        log.info("Statut campagne {} : {} → {}",
                campaignId, campaign.getStatus(), request.getStatus());

        campaign.setStatus(request.getStatus());
        return mapper.toCampaignResponse(campaignRepository.save(campaign));
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignStatsResponse getCampaignStats(Long campaignId) {
        findOrThrow(campaignId);

        List<Object[]> rows = applicationRepository.countByStatusForCampaign(campaignId);
        Map<ApplicationStatus, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((ApplicationStatus) row[0], (Long) row[1]);
        }

        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        return CampaignStatsResponse.builder()
                .campaignId(campaignId)
                .totalApplications((int) total)
                .draftCount(counts.getOrDefault(ApplicationStatus.DRAFT, 0L).intValue())
                .submittedCount(counts.getOrDefault(ApplicationStatus.SUBMITTED, 0L).intValue())
                .underReviewCount(counts.getOrDefault(ApplicationStatus.UNDER_ADMIN_REVIEW, 0L).intValue())
                .acceptedCount(counts.getOrDefault(ApplicationStatus.ACCEPTED, 0L).intValue())
                .rejectedCount(counts.getOrDefault(ApplicationStatus.REJECTED, 0L).intValue())
                .waitlistedCount(counts.getOrDefault(ApplicationStatus.AWAITING_CONFIRMATION, 0L).intValue())
                .build();
    }

    @Override
    @Transactional
    public void processScheduledTransitions() {
        LocalDate today = LocalDate.now();

        // UPCOMING → OPEN
        campaignRepository
                .findByStatusAndStartDateLessThanEqual(CampaignStatus.UPCOMING, today)
                .forEach(c -> {
                    c.setStatus(CampaignStatus.OPEN);
                    campaignRepository.save(c);
                    log.info("Campagne {} passée à OPEN", c.getAcademicYear());
                });

        // OPEN → CLOSED
        campaignRepository
                .findByStatusAndEndDateLessThan(CampaignStatus.OPEN, today)
                .forEach(c -> {
                    c.setStatus(CampaignStatus.CLOSED);
                    campaignRepository.save(c);
                    log.info("Campagne {} passée à CLOSED", c.getAcademicYear());
                });
    }

    private AdmissionCampaign findOrThrow(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne introuvable : id=" + id));
    }
}
