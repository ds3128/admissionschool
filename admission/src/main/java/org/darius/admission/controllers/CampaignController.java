package org.darius.admission.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.admission.common.dtos.requests.*;
import org.darius.admission.common.dtos.responses.*;
import org.darius.admission.services.CampaignService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admissions/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campagnes", description = "Gestion des campagnes d'admission")
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping
    @Operation(summary = "Lister les campagnes — public")
    public ResponseEntity<List<CampaignResponse>> getAll() {
        return ResponseEntity.ok(campaignService.getAllCampaigns());
    }

    @GetMapping("/current")
    @Operation(summary = "Campagne actuellement ouverte — public")
    public ResponseEntity<CampaignResponse> getCurrent() {
        return ResponseEntity.ok(campaignService.getCurrentCampaign());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une campagne — public")
    public ResponseEntity<CampaignResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getCampaignById(id));
    }

    @PostMapping
    @Operation(summary = "Créer une campagne — SUPER_ADMIN")
    public ResponseEntity<CampaignResponse> create(
            @Valid @RequestBody CreateCampaignRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(campaignService.createCampaign(request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Changer le statut d'une campagne — SUPER_ADMIN")
    public ResponseEntity<CampaignResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCampaignStatusRequest request
    ) {
        return ResponseEntity.ok(campaignService.updateCampaignStatus(id, request));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Statistiques d'une campagne — ADMIN_SCHOLAR")
    public ResponseEntity<CampaignStatsResponse> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getCampaignStats(id));
    }
}