package org.darius.userservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.userservice.common.dtos.requests.CreateStaffRequest;
import org.darius.userservice.common.dtos.responses.PageResponse;
import org.darius.userservice.common.dtos.responses.StaffResponse;
import org.darius.userservice.services.StaffService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/staff")
@RequiredArgsConstructor
@Tag(name = "Personnel administratif", description = "Gestion du personnel administratif")
public class StaffController {

    private final StaffService staffService;

    @GetMapping("/me")
    @Operation(summary = "Mon profil personnel")
    public ResponseEntity<StaffResponse> getMyProfile(
            @RequestHeader("X-User-Email") String userEmail
    ) {
        return ResponseEntity.ok(staffService.getMyStaffProfile(userEmail));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Profil personnel par ID")
    public ResponseEntity<StaffResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(staffService.getStaffById(id));
    }

    @GetMapping
    @Operation(summary = "Lister le personnel avec filtres")
    public ResponseEntity<PageResponse<StaffResponse>> getStaffMembers(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                staffService.getStaffMembers(departmentId, isActive, page, size)
        );
    }

    @PostMapping
    @Operation(summary = "Créer un profil personnel")
    public ResponseEntity<StaffResponse> createStaff(
            @RequestHeader("X-User-Id") String createdByUserId,
            @Valid @RequestBody CreateStaffRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(staffService.createStaff(request, createdByUserId));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Désactiver un membre du personnel")
    public ResponseEntity<StaffResponse> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(staffService.deactivateStaff(id));
    }
}