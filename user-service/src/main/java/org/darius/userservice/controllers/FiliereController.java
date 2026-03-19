package org.darius.userservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.userservice.common.dtos.requests.CreateFiliereRequest;
import org.darius.userservice.common.dtos.requests.UpdateFiliereStatusRequest;
import org.darius.userservice.common.dtos.responses.FiliereResponse;
import org.darius.userservice.common.dtos.responses.StudyLevelResponse;
import org.darius.userservice.common.enums.FiliereStatus;
import org.darius.userservice.services.FiliereService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/filieres")
@RequiredArgsConstructor
@Tag(name = "Filières", description = "Gestion des filières d'études")
public class FiliereController {

    private final FiliereService filiereService;

    @GetMapping
    @Operation(summary = "Lister les filières")
    public ResponseEntity<List<FiliereResponse>> getAllFilieres(
            @RequestParam(required = false) FiliereStatus status
    ) {
        return ResponseEntity.ok(filiereService.getAllFilieres(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une filière")
    public ResponseEntity<FiliereResponse> getFiliereById(@PathVariable Long id) {
        return ResponseEntity.ok(filiereService.getFiliereById(id));
    }

    @GetMapping("/{id}/levels")
    @Operation(summary = "Niveaux d'une filière")
    public ResponseEntity<List<StudyLevelResponse>> getStudyLevels(@PathVariable Long id) {
        return ResponseEntity.ok(filiereService.getStudyLevelsByFiliere(id));
    }

    @PostMapping
    @Operation(summary = "Créer une filière avec ses niveaux")
    public ResponseEntity<FiliereResponse> createFiliere(
            @Valid @RequestBody CreateFiliereRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(filiereService.createFiliere(request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Changer le statut d'une filière")
    public ResponseEntity<FiliereResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFiliereStatusRequest request
    ) {
        return ResponseEntity.ok(filiereService.updateFiliereStatus(id, request));
    }
}