package org.darius.payment.services.impls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.payment.common.dtos.requests.*;
import org.darius.payment.common.dtos.responses.*;
import org.darius.payment.entities.Payment;
import org.darius.payment.common.enums.PaymentStatus;
import org.darius.payment.common.enums.PaymentType;
import org.darius.payment.events.published.*;
import org.darius.payment.exceptions.DuplicateResourceException;
import org.darius.payment.exceptions.InvalidOperationException;
import org.darius.payment.exceptions.ResourceNotFoundException;
import org.darius.payment.kafka.PaymentEventProducer;
import org.darius.payment.mappers.PaymentMapper;
import org.darius.payment.repositories.InvoiceRepository;
import org.darius.payment.repositories.PaymentRepository;
import org.darius.payment.services.PaymentReferenceService;
import org.darius.payment.services.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository        paymentRepository;
    private final InvoiceRepository        invoiceRepository;
    private final PaymentReferenceService  referenceService;
    private final PaymentEventProducer     eventProducer;
    private final PaymentMapper mapper;

    // ── Frais de dossier ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse initiateAdmissionFee(String userId, InitiateAdmissionFeeRequest request) {
        // Vérifier pas de doublon COMPLETED
        if (paymentRepository.existsByApplicationIdAndStatus(
                request.getApplicationId(), PaymentStatus.COMPLETED)
        ) {
            throw new DuplicateResourceException(
                    "Les frais de dossier ont déjà été réglés pour cette candidature"
            );
        }

        Payment payment = Payment.builder()
                .userId(userId)
                .applicationId(request.getApplicationId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .type(PaymentType.FRAIS_DOSSIER)
                .status(PaymentStatus.PENDING)
                .method(request.getMethod())
                .reference(referenceService.generateReference())
                .description("Frais de dossier d'admission")
                .build();

        payment = paymentRepository.save(payment);
        log.info("Paiement frais dossier initié : ref={}, applicationId={}",
                payment.getReference(), request.getApplicationId());

        return mapper.toPaymentResponse(payment);
    }

    // ── Webhook ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void processWebhook(WebhookRequest request) {
        Payment payment = paymentRepository
                .findByExternalReference(request.getExternalReference())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Paiement introuvable pour la référence externe : " + request.getExternalReference()
                ));

        if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
            confirmPayment(payment);
        } else {
            failPayment(payment, request.getFailureReason());
        }
    }

    // ── Simulation (dev) ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse simulateConfirm(String paymentId) {
        Payment payment = findOrThrow(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidOperationException("Le paiement n'est pas en statut PENDING");
        }

        confirmPayment(payment);
        return mapper.toPaymentResponse(payment);
    }

    // ── Remboursement ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse refund(String paymentId, String adminId, RefundRequest request) {
        Payment original = findOrThrow(paymentId);

        if (original.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidOperationException("Seul un paiement COMPLETED peut être remboursé");
        }

        // Créer le paiement de remboursement
        Payment refundPayment = Payment.builder()
                .userId(original.getUserId())
                .applicationId(original.getApplicationId())
                .invoiceId(original.getInvoiceId())
                .amount(original.getAmount().negate())
                .currency(original.getCurrency())
                .type(PaymentType.REMBOURSEMENT)
                .status(PaymentStatus.COMPLETED)
                .method(original.getMethod())
                .reference(referenceService.generateReference())
                .description("Remboursement de " + original.getReference() + " — " + request.getReason())
                .paidAt(LocalDateTime.now())
                .build();

        refundPayment = paymentRepository.save(refundPayment);

        // Marquer l'original comme remboursé
        original.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(original);

        // Ajuster la facture si liée
        if (original.getInvoiceId() != null) {
            invoiceRepository.findById(original.getInvoiceId()).ifPresent(invoice -> {
                invoice.applyPayment(original.getAmount().negate());
                invoiceRepository.save(invoice);
            });
        }

        eventProducer.publishPaymentRefunded(
                PaymentRefundedEvent.builder()
                        .originalPaymentReference(original.getReference())
                        .refundPaymentReference(refundPayment.getReference())
                        .userId(original.getUserId())
                        .amount(original.getAmount())
                        .reason(request.getReason())
                        .build()
        );

        log.info("Remboursement effectué : original={}, refund={}",
                original.getReference(), refundPayment.getReference());
        return mapper.toPaymentResponse(refundPayment);
    }

    // ── Consultation ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String paymentId, String requesterId) {
        return mapper.toPaymentResponse(findOrThrow(paymentId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getPaymentHistory(String userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return buildPage(payments, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getMyPayments(String userId, int page, int size) {
        return getPaymentHistory(userId, page, size);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void confirmPayment(Payment payment) {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Mettre à jour la facture si liée
        if (payment.getInvoiceId() != null) {
            invoiceRepository.findById(payment.getInvoiceId()).ifPresent(invoice -> {
                invoice.applyPayment(payment.getAmount());
                invoiceRepository.save(invoice);

                if (invoice.getStatus().name().equals("PAID")) {
                    eventProducer.publishInvoicePaid(
                            InvoicePaidEvent.builder()
                                    .invoiceId(invoice.getId())
                                    .studentId(invoice.getStudentId())
                                    .academicYear(invoice.getAcademicYear())
                                    .amount(invoice.getNetAmount())
                                    .build()
                    );
                }
            });
        }

        eventProducer.publishPaymentCompleted(
                PaymentCompletedEvent.builder()
                        .paymentReference(payment.getReference())
                        .applicationId(payment.getApplicationId())
                        .invoiceId(payment.getInvoiceId())
                        .userId(payment.getUserId())
                        .amount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .type(payment.getType().name())
                        .paidAt(payment.getPaidAt())
                        .build()
        );

        log.info("Paiement confirmé : ref={}, type={}", payment.getReference(), payment.getType());
    }

    private void failPayment(Payment payment, String reason) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);

        eventProducer.publishPaymentFailed(
                PaymentFailedEvent.builder()
                        .paymentReference(payment.getReference())
                        .applicationId(payment.getApplicationId())
                        .userId(payment.getUserId())
                        .amount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .failureReason(reason)
                        .build()
        );

        log.warn("Paiement échoué : ref={}, raison={}", payment.getReference(), reason);
    }

    private Payment findOrThrow(String id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable : id=" + id));
    }

    private PageResponse<PaymentResponse> buildPage(Page<Payment> page, int pageNum, int size) {
        return PageResponse.<PaymentResponse>builder()
                .content(page.getContent().stream().map(mapper::toPaymentResponse).toList())
                .page(pageNum)
                .size(size)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}