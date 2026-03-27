package org.darius.payment.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.darius.payment.common.dtos.responses.*;
import org.darius.payment.services.FinanceStatsService;
import org.darius.payment.services.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments/admin")
@RequiredArgsConstructor
@Tag(name = "Administration Finance", description = "Tableaux de bord et rapports financiers")
public class AdminFinanceController {

    private final FinanceStatsService financeStatsService;
    private final InvoiceService      invoiceService;

    @GetMapping("/stats")
    @Operation(summary = "Statistiques financières globales - ADMIN_FINANCE")
    public ResponseEntity<FinanceStatsResponse> getStats(
            @RequestParam(required = false) String academicYear
    ) {
        return ResponseEntity.ok(financeStatsService.getStats(academicYear));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Factures en retard - ADMIN_FINANCE")
    public ResponseEntity<PageResponse<InvoiceResponse>> getOverdue(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(invoiceService.getOverdueInvoices(page, size));
    }
}