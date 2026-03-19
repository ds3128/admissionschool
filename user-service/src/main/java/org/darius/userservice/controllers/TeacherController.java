package org.darius.userservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.userservice.common.dtos.requests.CreateTeacherRequest;
import org.darius.userservice.common.dtos.requests.UpdateTeacherRequest;
import org.darius.userservice.common.dtos.responses.PageResponse;
import org.darius.userservice.common.dtos.responses.TeacherResponse;
import org.darius.userservice.common.dtos.responses.TeacherSummaryResponse;
import org.darius.userservice.services.TeacherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/teachers")
@RequiredArgsConstructor
@Tag(name = "Enseignants", description = "Gestion des profils enseignants")
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping("/me")
    @Operation(summary = "Mon profil enseignant")
    public ResponseEntity<TeacherResponse> getMyProfile(
            @RequestHeader("X-User-Email") String userEmail
    ) {
        return ResponseEntity.ok(teacherService.getMyTeacherProfile(userEmail));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Profil enseignant par ID")
    public ResponseEntity<TeacherResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(teacherService.getTeacherById(id));
    }

    @GetMapping
    @Operation(summary = "Lister les enseignants avec filtres")
    public ResponseEntity<PageResponse<TeacherSummaryResponse>> getTeachers(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                teacherService.getTeachers(departmentId, isActive, page, size)
        );
    }

    @PostMapping
    @Operation(summary = "Créer un profil enseignant")
    public ResponseEntity<TeacherResponse> createTeacher(
            @RequestHeader("X-User-Id") String createdByUserId,
            @Valid @RequestBody CreateTeacherRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teacherService.createTeacher(request, createdByUserId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un profil enseignant")
    public ResponseEntity<TeacherResponse> updateTeacher(
            @PathVariable String id,
            @Valid @RequestBody UpdateTeacherRequest request
    ) {
        return ResponseEntity.ok(teacherService.updateTeacher(id, request));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Désactiver un enseignant")
    public ResponseEntity<TeacherResponse> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(teacherService.deactivateTeacher(id));
    }

    @PutMapping("/{id}/reactivate")
    @Operation(summary = "Réactiver un enseignant")
    public ResponseEntity<TeacherResponse> reactivate(@PathVariable String id) {
        return ResponseEntity.ok(teacherService.reactivateTeacher(id));
    }
}