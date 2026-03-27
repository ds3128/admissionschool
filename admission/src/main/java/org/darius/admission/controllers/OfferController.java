package org.darius.admission.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.admission.common.dtos.requests.*;
import org.darius.admission.common.dtos.responses.*;
import org.darius.admission.common.enums.OfferLevel;
import org.darius.admission.services.OfferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admissions")
@RequiredArgsConstructor
@Tag(name = "Offres de formation", description = "Gestion des offres et documents requis")
public class OfferController {

    private final OfferService offerService;

    @GetMapping("/offers")
    @Operation(summary = "Lister les offres - public")
    public ResponseEntity<List<OfferSummaryResponse>> getOffers(
            @RequestParam Long campaignId,
            @RequestParam(required = false) OfferLevel level
    ) {
        return ResponseEntity.ok(offerService.getOffersByCampaign(campaignId, level));
    }

    @GetMapping("/offers/{id}")
    @Operation(summary = "Détail d'une offre avec documents requis - public")
    public ResponseEntity<OfferResponse> getOffer(@PathVariable Long id) {
        return ResponseEntity.ok(offerService.getOfferById(id));
    }

    @PostMapping("/offers")
    @Operation(summary = "Créer une offre - SUPER_ADMIN")
    public ResponseEntity<OfferResponse> createOffer(
            @Valid @RequestBody CreateOfferRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offerService.createOffer(request));
    }

    @PutMapping("/offers/{id}")
    @Operation(summary = "Modifier une offre - SUPER_ADMIN")
    public ResponseEntity<OfferResponse> updateOffer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOfferRequest request
    ) {
        return ResponseEntity.ok(offerService.updateOffer(id, request));
    }

    @GetMapping("/required-documents")
    @Operation(summary = "Documents requis pour une offre - public")
    public ResponseEntity<List<RequiredDocumentResponse>> getRequiredDocuments(
            @RequestParam Long offerId
    ) {
        return ResponseEntity.ok(offerService.getRequiredDocuments(offerId));
    }

    @PostMapping("/required-documents")
    @Operation(summary = "Définir un document requis - SUPER_ADMIN")
    public ResponseEntity<RequiredDocumentResponse> addRequiredDocument(
            @Valid @RequestBody AddRequiredDocumentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offerService.addRequiredDocument(request));
    }

    @DeleteMapping("/required-documents/{id}")
    @Operation(summary = "Supprimer un document requis - SUPER_ADMIN")
    public ResponseEntity<Void> removeRequiredDocument(@PathVariable Long id) {
        offerService.removeRequiredDocument(id);
        return ResponseEntity.noContent().build();
    }
}