package org.darius.admission.services.impls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.admission.common.dtos.responses.WaitlistEntryResponse;
import org.darius.admission.common.enums.WaitlistStatus;
import org.darius.admission.evens.published.WaitlistPromotedEvent;
import org.darius.admission.kafka.AdmissionEventProducer;
import org.darius.admission.mappers.AdmissionMapper;
import org.darius.admission.repositories.CandidateProfileRepository;
import org.darius.admission.repositories.WaitlistEntryRepository;
import org.darius.admission.services.WaitlistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistServiceImpl implements WaitlistService {

    private final WaitlistEntryRepository waitlistRepository;
    private final CandidateProfileRepository profileRepository;
    private final AdmissionEventProducer eventProducer;
    private final AdmissionMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<WaitlistEntryResponse> getWaitlistByOffer(Long offerId) {
        return waitlistRepository
                .findByOfferIdAndStatusOrderByRank(offerId, WaitlistStatus.WAITING)
                .stream()
                .map(mapper::toWaitlistEntryResponse)
                .toList();
    }

    @Override
    @Transactional
    public void promoteNextCandidate(Long offerId) {
        waitlistRepository
                .findFirstByOfferIdAndStatusOrderByRank(offerId, WaitlistStatus.WAITING)
                .ifPresent(entry -> {
                    LocalDateTime expiresAt = LocalDateTime.now().plusHours(48);
                    entry.setStatus(WaitlistStatus.PROMOTED);
                    entry.setPromotedAt(LocalDateTime.now());
                    entry.setExpiresAt(expiresAt);
                    entry.setNotifiedAt(LocalDateTime.now());
                    waitlistRepository.save(entry);

                    // Récupérer les infos du candidat pour la notification
                    String applicationId = entry.getChoice().getApplication().getId();
                    String userId        = entry.getChoice().getApplication().getUserId();
                    String filiereName   = entry.getChoice().getFiliereName();

                    profileRepository.findByApplication_Id(applicationId).ifPresent(profile ->
                            eventProducer.publishWaitlistPromoted(
                                    WaitlistPromotedEvent.builder()
                                            .applicationId(applicationId)
                                            .userId(userId)
                                            .personalEmail(profile.getPersonalEmail())
                                            .offerId(offerId)
                                            .filiereName(filiereName)
                                            .rank(entry.getRank())
                                            .expiresAt(expiresAt)
                                            .build()
                            )
                    );

                    log.info("Candidat promu depuis liste d'attente : rang={}, offerId={}", entry.getRank(), offerId);
                });
    }

    @Override
    @Transactional
    public void processExpiredPromotions() {
        waitlistRepository.findExpiredPromotions(LocalDateTime.now()).forEach(entry -> {
            entry.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(entry);
            log.info("Promotion expirée : waitlistEntryId={}", entry.getId());
            // Promouvoir le suivant
            promoteNextCandidate(entry.getOfferId());
        });
    }
}