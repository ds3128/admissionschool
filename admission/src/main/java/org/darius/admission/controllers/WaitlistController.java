package org.darius.admission.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.darius.admission.common.dtos.responses.WaitlistEntryResponse;
import org.darius.admission.services.WaitlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admissions/waitlist")
@RequiredArgsConstructor
@Tag(name = "Liste d'attente", description = "Gestion de la liste d'attente par offre")
public class WaitlistController {

    private final WaitlistService waitlistService;

    @GetMapping
    @Operation(summary = "Liste d'attente d'une offre — ADMIN_SCHOLAR")
    public ResponseEntity<List<WaitlistEntryResponse>> getWaitlist(
            @RequestParam Long offerId
    ) {
        return ResponseEntity.ok(waitlistService.getWaitlistByOffer(offerId));
    }
}