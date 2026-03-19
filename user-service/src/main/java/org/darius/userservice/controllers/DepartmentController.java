package org.darius.userservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.userservice.common.dtos.requests.AssignDepartmentHeadRequest;
import org.darius.userservice.common.dtos.requests.CreateDepartmentRequest;
import org.darius.userservice.common.dtos.requests.UpdateDepartmentRequest;
import org.darius.userservice.common.dtos.responses.DepartmentResponse;
import org.darius.userservice.services.DepartmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/departments")
@RequiredArgsConstructor
@Tag(name = "Départements", description = "Gestion des départements universitaires")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @Operation(summary = "Lister tous les départements")
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un département")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PostMapping
    @Operation(summary = "Créer un département")
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departmentService.createDepartment(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un département")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    @PutMapping("/{id}/head")
    @Operation(summary = "Désigner un chef de département")
    public ResponseEntity<DepartmentResponse> assignHead(
            @PathVariable Long id,
            @Valid @RequestBody AssignDepartmentHeadRequest request
    ) {
        return ResponseEntity.ok(departmentService.assignDepartmentHead(id, request));
    }
}