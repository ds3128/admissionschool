package org.darius.admission.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.admission.services.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfig {

    private final CampaignService              campaignService;
    private final OfferService                 offerService;
    private final WaitlistService              waitlistService;
    private final ThesisDirectorService        thesisDirectorService;
    private final ApplicationEvaluationService evaluationService;

    // Chaque jour à 00:05
    @Scheduled(cron = "0 5 0 * * *")
    public void processCampaignTransitions() {
        log.info("Scheduler — transitions campagnes");
        campaignService.processScheduledTransitions();
    }

    // Chaque jour à 00:10
    @Scheduled(cron = "0 10 0 * * *")
    public void processOfferTransitions() {
        log.info("Scheduler — transitions offres");
        offerService.processOfferStatusTransitions();
    }

    // Chaque heure
    @Scheduled(cron = "0 0 * * * *")
    public void processWaitlistExpirations() {
        log.info("Scheduler — expirations liste d'attente");
        waitlistService.processExpiredPromotions();
    }

    // Chaque jour à 06:00
    @Scheduled(cron = "0 0 6 * * *")
    public void processThesisExpirations() {
        log.info("Scheduler — expirations directeurs de thèse");
        thesisDirectorService.processExpiredApprovals();
        thesisDirectorService.sendReminders();
    }

    // Chaque heure
    @Scheduled(cron = "0 30 * * * *")
    public void processConfirmationExpirations() {
        log.info("Scheduler — confirmations automatiques");
        evaluationService.processExpiredConfirmations();
    }
}