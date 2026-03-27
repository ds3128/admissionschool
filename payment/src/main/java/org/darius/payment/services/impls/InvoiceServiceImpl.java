package org.darius.payment.services.impls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.payment.common.dtos.requests.*;
import org.darius.payment.common.dtos.responses.*;
import org.darius.payment.entities.*;
import org.darius.payment.common.enums.*;
import org.darius.payment.events.published.*;
import org.darius.payment.exceptions.*;
import org.darius.payment.kafka.PaymentEventProducer;
import org.darius.payment.mappers.PaymentMapper;
import org.darius.payment.repositories.*;
import org.darius.payment.services.InvoiceService;
import org.darius.payment.services.PaymentReferenceService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository         invoiceRepository;
    private final PaymentRepository         paymentRepository;
    private final PaymentScheduleRepository scheduleRepository;
    private final InstallmentRepository     installmentRepository;
    private final ScholarshipRepository     scholarshipRepository;
    private final PaymentReferenceService   referenceService;
    private final PaymentEventProducer      eventProducer;
    private final PaymentMapper             mapper;
    private final RestClient                restClient;

    // ── Génération ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Map<String, Integer> generateInvoices(GenerateInvoicesRequest request) {
        // Appel HTTP au User Service pour récupérer les étudiants actifs
        List<Map> students;
        try {
            students = restClient.get()
                    .uri("http://localhost:8082/users/students?status=ACTIVE&size=1000")
                    .retrieve()
                    .body(List.class);
            if (students == null) students = List.of();
        } catch (Exception ex) {
            log.error("Impossible de récupérer les étudiants du User Service : {}", ex.getMessage());
            throw new ServiceUnavailableException(
                    "Le User Service est indisponible — impossible de générer les factures"
            );
        }

        int generated = 0;
        int skipped   = 0;

        for (Map student : students) {
            String studentId = (String) student.get("id");
            if (studentId == null) { skipped++; continue; }

            // Vérifier doublon
            if (invoiceRepository.existsByStudentIdAndAcademicYearAndSemesterAndType(
                    studentId, request.getAcademicYear(), request.getSemester(), InvoiceType.SCOLARITE
            )) {
                skipped++;
                continue;
            }

            // Calculer la déduction bourse
            BigDecimal scholarshipDeduction = scholarshipRepository
                    .findByStudentIdAndStatus(studentId, ScholarshipStatus.ACTIVE)
                    .stream()
                    .map(s -> s.getAmount().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netAmount = request.getAmount().subtract(scholarshipDeduction);
            if (netAmount.compareTo(BigDecimal.ZERO) < 0) netAmount = BigDecimal.ZERO;

            Invoice invoice = Invoice.builder()
                    .studentId(studentId)
                    .academicYear(request.getAcademicYear())
                    .semester(request.getSemester())
                    .type(InvoiceType.SCOLARITE)
                    .amount(request.getAmount())
                    .scholarshipDeduction(scholarshipDeduction)
                    .netAmount(netAmount)
                    .remainingAmount(netAmount)
                    .dueDate(request.getDueDate())
                    .status(InvoiceStatus.PENDING)
                    .build();

            invoice = invoiceRepository.save(invoice);

            eventProducer.publishInvoiceGenerated(
                    InvoiceGeneratedEvent.builder()
                            .invoiceId(invoice.getId())
                            .studentId(studentId)
                            .academicYear(request.getAcademicYear())
                            .semester(request.getSemester())
                            .netAmount(netAmount)
                            .scholarshipDeduction(scholarshipDeduction)
                            .dueDate(request.getDueDate())
                            .build()
            );

            generated++;
        }

        log.info("Génération factures : {} générées, {} ignorées", generated, skipped);
        return Map.of("generated", generated, "skipped", skipped);
    }

    // ── Consultation ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getMyInvoices(String studentId) {
        return invoiceRepository.findByStudentIdOrderByDueDateDesc(studentId).stream()
                .map(mapper::toInvoiceResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(String invoiceId, String requesterId) {
        return mapper.toInvoiceResponse(findInvoiceOrThrow(invoiceId));
    }

    // ── Paiement ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse payInvoice(String invoiceId, String studentId, PayInvoiceRequest request) {
        Invoice invoice = findInvoiceOrThrow(invoiceId);

        if (!invoice.getStudentId().equals(studentId)) {
            throw new ForbiddenException("Cette facture ne vous appartient pas");
        }

        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvalidOperationException("Cette facture est déjà " + invoice.getStatus().name());
        }

        if (request.getAmount().compareTo(invoice.getRemainingAmount()) > 0) {
            throw new InvalidOperationException(
                    "Le montant ne peut pas dépasser le solde restant (" + invoice.getRemainingAmount() + ")"
            );
        }

        Payment payment = Payment.builder()
                .userId(studentId)
                .invoiceId(invoiceId)
                .amount(request.getAmount())
                .currency(invoice.getCurrency())
                .type(PaymentType.FRAIS_SCOLARITE)
                .status(PaymentStatus.COMPLETED) // Simplifié — en prod passerait par passerelle
                .method(request.getMethod())
                .reference(referenceService.generateReference())
                .description("Paiement facture " + invoice.getAcademicYear() + " " + invoice.getSemester())
                .paidAt(LocalDateTime.now())
                .build();

        payment = paymentRepository.save(payment);

        // Mettre à jour la facture
        invoice.applyPayment(request.getAmount());
        invoiceRepository.save(invoice);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            eventProducer.publishInvoicePaid(
                    InvoicePaidEvent.builder()
                            .invoiceId(invoiceId)
                            .studentId(studentId)
                            .academicYear(invoice.getAcademicYear())
                            .amount(invoice.getNetAmount())
                            .build()
            );
        }

        eventProducer.publishPaymentCompleted(
                PaymentCompletedEvent.builder()
                        .paymentReference(payment.getReference())
                        .invoiceId(invoiceId)
                        .userId(studentId)
                        .amount(request.getAmount())
                        .currency(invoice.getCurrency())
                        .type(PaymentType.FRAIS_SCOLARITE.name())
                        .paidAt(payment.getPaidAt())
                        .build()
        );

        return mapper.toPaymentResponse(payment);
    }

    // ── Échéancier ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentScheduleResponse createSchedule(
            String invoiceId, String adminId, CreateScheduleRequest request
    ) {
        Invoice invoice = findInvoiceOrThrow(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvalidOperationException("Impossible de créer un échéancier sur cette facture");
        }

        if (scheduleRepository.existsByInvoice_Id(invoiceId)) {
            throw new DuplicateResourceException("Un échéancier existe déjà pour cette facture");
        }

        BigDecimal total = request.getInstallments().stream()
                .map(CreateScheduleRequest.InstallmentRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(invoice.getNetAmount()) != 0) {
            throw new InvalidOperationException(
                    "La somme des échéances (" + total + ") doit être égale au montant net (" + invoice.getNetAmount() + ")"
            );
        }

        PaymentSchedule schedule = PaymentSchedule.builder()
                .invoice(invoice)
                .totalInstallments(request.getInstallments().size())
                .totalAmount(total)
                .createdBy(adminId)
                .build();

        schedule = scheduleRepository.save(schedule);

        int num = 1;
        for (CreateScheduleRequest.InstallmentRequest ir : request.getInstallments()) {
            Installment inst = Installment.builder()
                    .schedule(schedule)
                    .installmentNumber(num++)
                    .amount(ir.getAmount())
                    .dueDate(ir.getDueDate())
                    .status(InstallmentStatus.PENDING)
                    .build();
            installmentRepository.save(inst);
        }

        invoice.setHasSchedule(true);
        invoiceRepository.save(invoice);

        return mapper.toScheduleResponse(scheduleRepository.findById(schedule.getId()).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentScheduleResponse getSchedule(String invoiceId, String requesterId) {
        PaymentSchedule schedule = scheduleRepository.findByInvoice_Id(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun échéancier pour cette facture"));
        return mapper.toScheduleResponse(schedule);
    }

    // ── Annulation ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public InvoiceResponse cancelInvoice(String invoiceId, String adminId) {
        Invoice invoice = findInvoiceOrThrow(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvalidOperationException("Une facture PAID ne peut pas être annulée");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        return mapper.toInvoiceResponse(invoiceRepository.save(invoice));
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> getOverdueInvoices(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("dueDate").ascending());
        var result   = invoiceRepository.findByStatusOrderByDueDateAsc(InvoiceStatus.OVERDUE, pageable);
        return PageResponse.<InvoiceResponse>builder()
                .content(result.getContent().stream().map(mapper::toInvoiceResponse).toList())
                .page(page).size(size)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    // ── Schedulers ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void processOverdueInvoices() {
        LocalDate today     = LocalDate.now();
        LocalDate threshold = today.minusDays(30);

        // PENDING/PARTIAL → OVERDUE
        invoiceRepository.findOverdueInvoices(today).forEach(invoice -> {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
            eventProducer.publishInvoiceOverdue(
                    InvoiceOverdueEvent.builder()
                            .invoiceId(invoice.getId())
                            .studentId(invoice.getStudentId())
                            .remainingAmount(invoice.getRemainingAmount())
                            .dueDate(invoice.getDueDate())
                            .academicYear(invoice.getAcademicYear())
                            .build()
            );
            log.info("Facture {} passée OVERDUE", invoice.getId());
        });

        // OVERDUE depuis > 30j → blocage étudiant
        invoiceRepository.findCriticalOverdueInvoices(threshold).forEach(invoice -> {
            int days = (int) (today.toEpochDay() - invoice.getDueDate().toEpochDay());
            eventProducer.publishStudentPaymentBlocked(
                    StudentPaymentBlockedEvent.builder()
                            .studentId(invoice.getStudentId())
                            .userId(invoice.getStudentId())
                            .invoiceId(invoice.getId())
                            .amount(invoice.getRemainingAmount())
                            .overdueDays(days)
                            .academicYear(invoice.getAcademicYear())
                            .build()
            );
            log.warn("Blocage étudiant {} pour impayé critique ({} jours)", invoice.getStudentId(), days);
        });

        // Installments en retard
        installmentRepository.findOverdueInstallments(today).forEach(inst -> {
            inst.setStatus(InstallmentStatus.OVERDUE);
            installmentRepository.save(inst);
        });
    }

    @Override
    @Transactional
    public void sendPaymentReminders() {
        LocalDate today      = LocalDate.now();
        LocalDate remindDate = today.plusDays(7);

        invoiceRepository.findInvoicesDueSoon(today, remindDate).forEach(invoice ->
                        log.info("Rappel paiement envoyé — studentId={}, dueDate={}, remaining={}",
                                invoice.getStudentId(), invoice.getDueDate(), invoice.getRemainingAmount())
                // TODO : event Notification Service quand implémenté
        );
    }

    private Invoice findInvoiceOrThrow(String id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable : id=" + id));
    }
}
