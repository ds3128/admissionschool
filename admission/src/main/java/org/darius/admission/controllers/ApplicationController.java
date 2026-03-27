package org.darius.admission.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.admission.common.dtos.requests.*;
import org.darius.admission.common.dtos.responses.*;
import org.darius.admission.services.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admissions/applications")
@RequiredArgsConstructor
@Tag(name = "Candidatures", description = "Flux candidat - création, soumission, confirmation")
public class ApplicationController {

    private final ApplicationService applicationService;

    // ── Consultation ─────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Mes candidatures")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(applicationService.getMyApplications(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une candidature")
    public ResponseEntity<ApplicationResponse> getById(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(applicationService.getApplicationById(id, userId));
    }

    // ── Création ─────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Créer une candidature en brouillon")
    public ResponseEntity<ApplicationResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateApplicationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.createApplication(userId, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Retirer une candidature (DRAFT uniquement)")
    public ResponseEntity<Void> withdraw(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId
    ) {
        applicationService.withdrawApplication(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Profil candidat ───────────────────────────────────────────────────────

    @GetMapping("/{id}/profile")
    @Operation(summary = "Consulter le profil candidat")
    public ResponseEntity<CandidateProfileResponse> getProfile(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(
                applicationService.getApplicationById(id, userId).getCandidateProfile()
        );
    }

    @PutMapping("/{id}/profile")
    @Operation(summary = "Remplir le profil candidat")
    public ResponseEntity<CandidateProfileResponse> updateProfile(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateCandidateProfileRequest request
    ) {
        return ResponseEntity.ok(
                applicationService.updateCandidateProfile(id, userId, request)
        );
    }

    // ── Choix ─────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/choices")
    @Operation(summary = "Ajouter un choix de formation")
    public ResponseEntity<ChoiceResponse> addChoice(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AddChoiceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.addChoice(id, userId, request));
    }

    @DeleteMapping("/{id}/choices/{choiceId}")
    @Operation(summary = "Retirer un choix")
    public ResponseEntity<Void> removeChoice(
            @PathVariable String id,
            @PathVariable Long choiceId,
            @RequestHeader("X-User-Id") String userId
    ) {
        applicationService.removeChoice(id, choiceId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/choices/reorder")
    @Operation(summary = "Réordonner les choix")
    public ResponseEntity<ApplicationResponse> reorderChoices(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ReorderChoicesRequest request
    ) {
        return ResponseEntity.ok(applicationService.reorderChoices(id, userId, request));
    }

    // ── Documents ─────────────────────────────────────────────────────────────

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader un document")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @RequestPart("file") MultipartFile file,
            @RequestParam String documentType
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.uploadDocument(id, userId, file, documentType));
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @Operation(summary = "Supprimer un document")
    public ResponseEntity<Void> removeDocument(
            @PathVariable String id,
            @PathVariable Long documentId,
            @RequestHeader("X-User-Id") String userId
    ) {
        applicationService.removeDocument(id, documentId, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Paiement ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/payment")
    @Operation(summary = "Initier le paiement des frais de dossier")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.initiatePayment(id, userId));
    }

    // ── Soumission ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/submit")
    @Operation(summary = "Soumettre la candidature")
    public ResponseEntity<ApplicationResponse> submit(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(applicationService.submitApplication(id, userId));
    }

    // ── Confirmation ──────────────────────────────────────────────────────────

    @GetMapping("/{id}/confirmation")
    @Operation(summary = "Statut de confirmation - choix acceptés + délai restant")
    public ResponseEntity<ConfirmationResponse> getConfirmation(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(applicationService.getConfirmationStatus(id, userId));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirmer un choix de formation")
    public ResponseEntity<ApplicationResponse> confirmChoice(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ConfirmChoiceRequest request
    ) {
        return ResponseEntity.ok(applicationService.confirmChoice(id, userId, request));
    }
}