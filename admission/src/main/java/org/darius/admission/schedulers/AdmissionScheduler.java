package org.darius.admission.schedulers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.admission.services.ApplicationService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class AdmissionScheduler {

    private final ApplicationService applicationService;

    /**
     * Toutes les heures : expire les candidatures en attente de confirmation
     * dont le délai est dépassé → status CONFIRMED automatiquement ou EXPIRED
     */
    @Scheduled(fixedDelay = 3_600_000)
    public void processExpiredConfirmations() {
        log.info("Scheduler - vérification des confirmations expirées");
        try {
            int processed = applicationService.autoExpireConfirmations();
            log.info("Confirmations expirées traitées : {}", processed);
        } catch (Exception ex) {
            log.error("Erreur scheduler autoExpireConfirmations : {}", ex.getMessage(), ex);
        }
    }

    /**
     * Toutes les heures : promouvoir les candidatures en liste d'attente
     * si des places se libèrent (suite à des retraits/expirations)
     */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 300_000)
    public void processWaitlistPromotions() {
        log.info("Scheduler - traitement liste d'attente");
        try {
            int promoted = applicationService.promoteFromWaitlist();
            log.info("Candidats promus depuis liste d'attente : {}", promoted);
        } catch (Exception ex) {
            log.error("Erreur scheduler processWaitlistPromotions : {}", ex.getMessage(), ex);
        }
    }
}

