package org.darius.payment.services.impls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.payment.common.dtos.responses.FinanceStatsResponse;
import org.darius.payment.entities.Invoice;
import org.darius.payment.repositories.InvoiceRepository;
import org.darius.payment.repositories.PaymentRepository;
import org.darius.payment.repositories.ScholarshipRepository;
import org.darius.payment.services.FinanceStatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceStatsServiceImpl implements FinanceStatsService {

    private final InvoiceRepository    invoiceRepository;
    private final ScholarshipRepository scholarshipRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public FinanceStatsResponse getStats(String academicYear) {

        // Comptages factures par statut
        List<Object[]> statusCounts = invoiceRepository.countByStatus();
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : statusCounts) {
            counts.put(row[0].toString(), (Long) row[1]);
        }

        // Total collecté par type — depuis les paiements COMPLETED
        List<Object[]> collectedRows = paymentRepository.sumCollectedByType(null, null);
        Map<String, BigDecimal> collectedByType = new HashMap<>();
        BigDecimal totalCollected = BigDecimal.ZERO;
        for (Object[] row : collectedRows) {
            BigDecimal amount = (BigDecimal) row[1];
            collectedByType.put(row[0].toString(), amount);
            totalCollected = totalCollected.add(amount);
        }

        // Étudiants bloqués — le Payment Service connaît lui-même
        // les factures OVERDUE depuis > 30 jours
        LocalDate threshold = LocalDate.now().minusDays(30);
        int blockedCount = invoiceRepository.findCriticalOverdueInvoices(threshold).size();

        // Bourses actives
        long activeScholarships = scholarshipRepository.countActiveScholarships();
        BigDecimal totalScholarships = scholarshipRepository.sumActiveScholarshipAmounts();
        if (totalScholarships == null) totalScholarships = BigDecimal.ZERO;

        return FinanceStatsResponse.builder()
                .academicYear(academicYear)
                .totalCollected(totalCollected)
                .totalPending(BigDecimal.ZERO)       // factures PENDING — à calculer si besoin
                .totalOverdue(BigDecimal.ZERO)       // factures OVERDUE — à calculer si besoin
                .totalScholarships(totalScholarships)
                .invoicePaidCount(counts.getOrDefault("PAID", 0L).intValue())
                .invoiceOverdueCount(counts.getOrDefault("OVERDUE", 0L).intValue())
                .invoicePendingCount(counts.getOrDefault("PENDING", 0L).intValue())
                .blockedStudentsCount(blockedCount)
                .activeScholarshipsCount((int) activeScholarships)
                .collectedByType(collectedByType)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getBlockedStudentIds() {
        LocalDate threshold = LocalDate.now().minusDays(30);
        return invoiceRepository.findCriticalOverdueInvoices(threshold)
                .stream()
                .map(Invoice::getStudentId)
                .distinct()
                .toList();
    }
}
