package org.darius.payment.common.configs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.payment.services.InvoiceService;
import org.darius.payment.services.ScholarshipService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfig {

    private final InvoiceService    invoiceService;
    private final ScholarshipService scholarshipService;

    // Chaque jour à 02:00 — détection impayés
    @Scheduled(cron = "0 0 2 * * *")
    public void processOverdueInvoices() {
        log.info("Scheduler — vérification factures en retard");
        invoiceService.processOverdueInvoices();
    }

    // 1er du mois à 08:00 — versements bourses
    @Scheduled(cron = "0 0 8 1 * *")
    public void processDisbursements() {
        log.info("Scheduler — versements bourses du mois");
        scholarshipService.processDisbursements();
    }

    // 1er juillet à 06:00 — renouvellement bourses mérite
    @Scheduled(cron = "0 0 6 1 7 *")
    public void processScholarshipRenewals() {
        int nextYear = Year.now().getValue() + 1;
        String nextAcademicYear = Year.now().getValue() + "-" + nextYear;
        log.info("Scheduler — renouvellement bourses mérite pour {}", nextAcademicYear);
        scholarshipService.processAnnualMeritRenewal(nextAcademicYear);
    }

    // Chaque lundi à 09:00 — rappels de paiement
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendPaymentReminders() {
        log.info("Scheduler — rappels de paiement");
        invoiceService.sendPaymentReminders();
    }
}