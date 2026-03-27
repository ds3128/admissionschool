package org.darius.notification.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.darius.notification.dtos.requests.UpdatePreferenceRequest;
import org.darius.notification.dtos.responses.PreferenceResponse;
import org.darius.notification.services.PreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "Préférences de notification")
public class PreferenceController {

    private final PreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Mes préférences de notification")
    public ResponseEntity<PreferenceResponse> getPreferences(
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(preferenceService.getPreferences(userId));
    }

    @PutMapping
    @Operation(summary = "Mettre à jour mes préférences")
    public ResponseEntity<PreferenceResponse> updatePreferences(
            @RequestHeader("X-User-Id")       String                  userId,
            @RequestBody UpdatePreferenceRequest request
    ) {
        return ResponseEntity.ok(preferenceService.updatePreferences(userId, request));
    }
}
