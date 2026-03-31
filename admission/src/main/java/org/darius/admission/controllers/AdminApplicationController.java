package org.darius.admission.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.admission.common.dtos.requests.AdminReviewRequest;
import org.darius.admission.common.dtos.requests.RequestAdditionalDocsRequest;
import org.darius.admission.common.dtos.responses.AdminStatsResponse;
import org.darius.admission.common.dtos.responses.ApplicationResponse;
import org.darius.admission.common.dtos.responses.ApplicationSummaryResponse;
import org.darius.admission.common.dtos.responses.PageResponse;
import org.darius.admission.common.enums.ApplicationStatus;
import org.darius.admission.services.AdminApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admissions/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Gestion administrative des candidatures")
public class AdminApplicationController {

    private final AdminApplicationService adminService;

    @GetMapping("/applications")
    @Operation(summary = "Lister toutes les candidatures - ADMIN_SCHOLAR")
    public ResponseEntity<PageResponse<ApplicationSummaryResponse>> getAll(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.getAllApplications(status, campaignId, page, size));
    }

    @GetMapping("/applications/{id}")
    @Operation(summary = "Dossier complet - ADMIN_SCHOLAR")
    public ResponseEntity<ApplicationResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(adminService.getApplicationByIdAdmin(id));
    }

    @PutMapping("/applications/{id}/admin-review")
    @Operation(summary = "Valider ou refuser administrativement - ADMIN_SCHOLAR")
    public ResponseEntity<ApplicationResponse> adminReview(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String adminUserId,
            @Valid @RequestBody AdminReviewRequest request
    ) {
        return ResponseEntity.ok(adminService.processAdminReview(id, adminUserId, request));
    }

    @PostMapping("/applications/{id}/request-docs")
    @Operation(summary = "Demander des documents supplémentaires - ADMIN_SCHOLAR")
    public ResponseEntity<ApplicationResponse> requestDocs(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String adminUserId,
            @Valid @RequestBody RequestAdditionalDocsRequest request
    ) {
        return ResponseEntity.ok(
                adminService.requestAdditionalDocuments(id, adminUserId, request)
        );
    }

    @PutMapping("/applications/{id}/forward-commission")
    @Operation(summary = "Transmettre manuellement à la commission - ADMIN_SCHOLAR")
    public ResponseEntity<ApplicationResponse> forwardToCommission(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String adminUserId
    ) {
        return ResponseEntity.ok(adminService.forwardToCommission(id, adminUserId));
    }

    @Operation(summary = "Statistiques globales de la campagne")
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats(
            @RequestParam(required = false) Long campaignId
    ) {
        return ResponseEntity.ok(adminService.getStats(campaignId));
    }

    @Operation(summary = "Export CSV des candidatures")
    @GetMapping("/export")
    public ResponseEntity<org.springframework.core.io.Resource> exportCsv(
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) String status
    ) {
        byte[] csv = adminService.exportToCsv(campaignId, status);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=candidatures.csv")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                .body(new org.springframework.core.io.ByteArrayResource(csv));
    }

}