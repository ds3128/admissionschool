package org.darius.admission.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.admission.common.dtos.requests.AddCommissionMemberRequest;
import org.darius.admission.common.dtos.requests.CastVoteRequest;
import org.darius.admission.common.dtos.requests.ValidateDecisionRequest;
import org.darius.admission.common.dtos.responses.*;
import org.darius.admission.services.CommissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admissions/commissions")
@RequiredArgsConstructor
@Tag(name = "Commissions", description = "Gestion des commissions pédagogiques et votes")
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping
    @Operation(summary = "Lister les commissions — ADMIN_SCHOLAR")
    public ResponseEntity<List<CommissionResponse>> getAll() {
        return ResponseEntity.ok(commissionService.getAllCommissions());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une commission")
    public ResponseEntity<CommissionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commissionService.getCommissionById(id));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Ajouter un membre — SUPER_ADMIN")
    public ResponseEntity<CommissionResponse> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddCommissionMemberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commissionService.addMember(id, request));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "Retirer un membre — SUPER_ADMIN")
    public ResponseEntity<CommissionResponse> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(commissionService.removeMember(id, memberId));
    }

    @GetMapping("/{id}/choices")
    @Operation(summary = "Dossiers à examiner — TEACHER (membre)")
    public ResponseEntity<List<ChoiceResponse>> getPendingChoices(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String teacherId
    ) {
        return ResponseEntity.ok(commissionService.getPendingChoicesForCommission(id, teacherId));
    }

    @PostMapping("/{id}/votes")
    @Operation(summary = "Voter sur un choix — TEACHER (membre)")
    public ResponseEntity<VoteResponse> castVote(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String teacherId,
            @Valid @RequestBody CastVoteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commissionService.castVote(id, teacherId, request));
    }

    @GetMapping("/{id}/choices/{choiceId}/vote-result")
    @Operation(summary = "Résultat du vote pour un choix — TEACHER (président)")
    public ResponseEntity<VoteResultResponse> getVoteResult(
            @PathVariable Long id,
            @PathVariable Long choiceId
    ) {
        return ResponseEntity.ok(commissionService.getVoteResult(id, choiceId));
    }

    @PostMapping("/{id}/choices/{choiceId}/validate")
    @Operation(summary = "Valider la décision finale — TEACHER (président)")
    public ResponseEntity<ChoiceResponse> validateDecision(
            @PathVariable Long id,
            @PathVariable Long choiceId,
            @RequestHeader("X-User-Id") String presidentId,
            @Valid @RequestBody ValidateDecisionRequest request
    ) {
        return ResponseEntity.ok(
                commissionService.validateDecision(id, choiceId, presidentId, request)
        );
    }
}