package org.darius.payment.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.payment.common.dtos.requests.*;
import org.darius.payment.common.dtos.responses.*;
import org.darius.payment.services.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments/invoices")
@RequiredArgsConstructor
@Tag(name = "Factures", description = "Facturation des frais de scolarité")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/me")
    @Operation(summary = "Mes factures — STUDENT")
    public ResponseEntity<List<InvoiceResponse>> getMyInvoices(
            @RequestHeader("X-User-Id") String studentId
    ) {
        return ResponseEntity.ok(invoiceService.getMyInvoices(studentId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une facture")
    public ResponseEntity<InvoiceResponse> getById(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String requesterId
    ) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id, requesterId));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Payer une facture — STUDENT")
    public ResponseEntity<PaymentResponse> pay(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String studentId,
            @Valid @RequestBody PayInvoiceRequest request
    ) {
        return ResponseEntity.ok(invoiceService.payInvoice(id, studentId, request));
    }

    @PostMapping("/generate")
    @Operation(summary = "Générer les factures d'une cohorte — ADMIN_FINANCE")
    public ResponseEntity<Map<String, Integer>> generate(
            @Valid @RequestBody GenerateInvoicesRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.generateInvoices(request));
    }

    @PostMapping("/{id}/schedule")
    @Operation(summary = "Créer un échéancier — ADMIN_FINANCE")
    public ResponseEntity<PaymentScheduleResponse> createSchedule(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String adminId,
            @Valid @RequestBody CreateScheduleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.createSchedule(id, adminId, request));
    }

    @GetMapping("/{id}/schedule")
    @Operation(summary = "Consulter l'échéancier d'une facture")
    public ResponseEntity<PaymentScheduleResponse> getSchedule(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String requesterId
    ) {
        return ResponseEntity.ok(invoiceService.getSchedule(id, requesterId));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Annuler une facture — ADMIN_FINANCE")
    public ResponseEntity<InvoiceResponse> cancel(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String adminId
    ) {
        return ResponseEntity.ok(invoiceService.cancelInvoice(id, adminId));
    }
}