package org.darius.admission.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.admission.common.dtos.requests.ThesisDirectorResponseRequest;
import org.darius.admission.common.dtos.responses.ThesisApprovalResponse;
import org.darius.admission.services.ThesisDirectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admissions/thesis-approvals")
@RequiredArgsConstructor
@Tag(name = "Directeurs de thèse", description = "Accord directeur de thèse pour les candidatures Doctorat")
public class ThesisDirectorController {

    private final ThesisDirectorService thesisDirectorService;

    @GetMapping
    @Operation(summary = "Demandes d'accord en attente - TEACHER (HDR)")
    public ResponseEntity<List<ThesisApprovalResponse>> getPending(
            @RequestHeader("X-User-Id") String directorId
    ) {
        return ResponseEntity.ok(thesisDirectorService.getPendingApprovals(directorId));
    }

    @PutMapping("/{id}/respond")
    @Operation(summary = "Répondre à une demande d'accord - TEACHER (HDR)")
    public ResponseEntity<ThesisApprovalResponse> respond(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String directorId,
            @Valid @RequestBody ThesisDirectorResponseRequest request
    ) {
        return ResponseEntity.ok(
                thesisDirectorService.respondToApproval(id, directorId, request)
        );
    }
}
