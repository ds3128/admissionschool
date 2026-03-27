package org.darius.payment.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.payment.common.dtos.requests.*;
import org.darius.payment.common.dtos.responses.*;
import org.darius.payment.services.ScholarshipService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments/scholarships")
@RequiredArgsConstructor
@Tag(name = "Bourses", description = "Gestion des bourses étudiantes")
public class ScholarshipController {

    private final ScholarshipService scholarshipService;

    @GetMapping("/me")
    @Operation(summary = "Mes bourses actives - STUDENT")
    public ResponseEntity<List<ScholarshipResponse>> getMyScholarships(
            @RequestHeader("X-User-Id") String studentId
    ) {
        return ResponseEntity.ok(scholarshipService.getMyScholarships(studentId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une bourse - ADMIN_FINANCE")
    public ResponseEntity<ScholarshipResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(scholarshipService.getScholarshipById(id));
    }

    @GetMapping
    @Operation(summary = "Lister les bourses — ADMIN_FINANCE")
    public ResponseEntity<List<ScholarshipResponse>> getAll(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(scholarshipService.getScholarships(studentId, type, status));
    }

    @PostMapping
    @Operation(summary = "Attribuer une bourse — ADMIN_FINANCE")
    public ResponseEntity<ScholarshipResponse> create(
            @RequestHeader("X-User-Id") String adminId,
            @Valid @RequestBody CreateScholarshipRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scholarshipService.createScholarship(adminId, request));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activer une bourse — ADMIN_FINANCE")
    public ResponseEntity<ScholarshipResponse> activate(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String adminId
    ) {
        return ResponseEntity.ok(scholarshipService.activateScholarship(id, adminId));
    }

    @PutMapping("/{id}/suspend")
    @Operation(summary = "Suspendre une bourse — ADMIN_FINANCE")
    public ResponseEntity<ScholarshipResponse> suspend(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String adminId,
            @Valid @RequestBody SuspendScholarshipRequest request
    ) {
        return ResponseEntity.ok(scholarshipService.suspendScholarship(id, adminId, request));
    }

    @PutMapping("/{id}/terminate")
    @Operation(summary = "Terminer définitivement une bourse — ADMIN_FINANCE")
    public ResponseEntity<ScholarshipResponse> terminate(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String adminId
    ) {
        return ResponseEntity.ok(scholarshipService.terminateScholarship(id, adminId));
    }

    @GetMapping("/{id}/disbursements")
    @Operation(summary = "Historique des versements d'une bourse")
    public ResponseEntity<List<DisbursementResponse>> getDisbursements(@PathVariable Long id) {
        return ResponseEntity.ok(scholarshipService.getDisbursements(id));
    }
}