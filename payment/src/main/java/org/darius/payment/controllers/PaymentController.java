package org.darius.payment.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.payment.common.dtos.requests.*;
import org.darius.payment.common.dtos.responses.*;
import org.darius.payment.services.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Paiements", description = "Transactions de paiement")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/admission-fees")
    @Operation(summary = "Initier le paiement des frais de dossier — CANDIDATE")
    public ResponseEntity<PaymentResponse> initiateAdmissionFee(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody InitiateAdmissionFeeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.initiateAdmissionFee(userId, request));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Confirmation passerelle externe — public")
    public ResponseEntity<Void> webhook(@RequestBody WebhookRequest request) {
        paymentService.processWebhook(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/simulate-confirm")
    @Operation(summary = "Simuler confirmation — dev uniquement")
    public ResponseEntity<PaymentResponse> simulateConfirm(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.simulateConfirm(id));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Rembourser un paiement — ADMIN_FINANCE")
    public ResponseEntity<PaymentResponse> refund(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String adminId,
            @Valid @RequestBody RefundRequest request
    ) {
        return ResponseEntity.ok(paymentService.refund(id, adminId, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un paiement")
    public ResponseEntity<PaymentResponse> getById(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String requesterId
    ) {
        return ResponseEntity.ok(paymentService.getPaymentById(id, requesterId));
    }

    @GetMapping("/me")
    @Operation(summary = "Mes paiements")
    public ResponseEntity<PageResponse<PaymentResponse>> getMyPayments(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(paymentService.getMyPayments(userId, page, size));
    }

    @GetMapping("/history")
    @Operation(summary = "Historique paiements — ADMIN_FINANCE")
    public ResponseEntity<PageResponse<PaymentResponse>> getHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(userId, page, size));
    }
}