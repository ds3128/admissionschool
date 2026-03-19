package org.darius.userservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.userservice.common.dtos.requests.TransferStudentRequest;
import org.darius.userservice.common.dtos.requests.UpdateStudentStatusRequest;
import org.darius.userservice.common.dtos.responses.PageResponse;
import org.darius.userservice.common.dtos.responses.StudentAcademicHistoryResponse;
import org.darius.userservice.common.dtos.responses.StudentResponse;
import org.darius.userservice.common.dtos.responses.StudentSummaryResponse;
import org.darius.userservice.common.enums.StudentStatus;
import org.darius.userservice.services.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/students")
@RequiredArgsConstructor
@Tag(name = "Étudiants", description = "Gestion des dossiers étudiants")
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/me")
    @Operation(summary = "Mon dossier étudiant")
    public ResponseEntity<StudentResponse> getMyProfile(
            @RequestHeader("X-User-Email") String userEmail
    ) {
        return ResponseEntity.ok(studentService.getMyStudentProfile(userEmail));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dossier étudiant par ID")
    public ResponseEntity<StudentResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping("/number/{studentNumber}")
    @Operation(summary = "Dossier étudiant par matricule")
    public ResponseEntity<StudentResponse> getByNumber(@PathVariable String studentNumber) {
        return ResponseEntity.ok(studentService.getStudentByNumber(studentNumber));
    }

    @GetMapping
    @Operation(summary = "Lister les étudiants avec filtres")
    public ResponseEntity<PageResponse<StudentSummaryResponse>> getStudents(
            @RequestParam(required = false) Long filiereId,
            @RequestParam(required = false) Long levelId,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                studentService.getStudents(filiereId, levelId, status, page, size)
        );
    }

    @PutMapping("/{id}/promote")
    @Operation(summary = "Promouvoir un étudiant au niveau supérieur")
    public ResponseEntity<StudentResponse> promote(@PathVariable String id) {
        return ResponseEntity.ok(studentService.promoteStudent(id));
    }

    @PutMapping("/{id}/graduate")
    @Operation(summary = "Marquer un étudiant comme diplômé")
    public ResponseEntity<StudentResponse> graduate(@PathVariable String id) {
        return ResponseEntity.ok(studentService.graduateStudent(id));
    }

    @PutMapping("/{id}/transfer")
    @Operation(summary = "Transférer un étudiant vers une autre filière")
    public ResponseEntity<StudentResponse> transfer(
            @PathVariable String id,
            @Valid @RequestBody TransferStudentRequest request
    ) {
        return ResponseEntity.ok(studentService.transferStudent(id, request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Changer le statut d'un étudiant")
    public ResponseEntity<StudentResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateStudentStatusRequest request
    ) {
        return ResponseEntity.ok(studentService.updateStudentStatus(id, request));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Historique académique d'un étudiant")
    public ResponseEntity<List<StudentAcademicHistoryResponse>> getHistory(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(studentService.getAcademicHistory(id));
    }
}