package org.darius.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.course.dtos.requests.CreateSemesterRequest;
import org.darius.course.dtos.responses.SemesterResponse;
import org.darius.course.dtos.responses.StudentProgressResponse;
import org.darius.course.services.SemesterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses/semesters")
@RequiredArgsConstructor
@Tag(name = "Semestres")
public class SemesterController {

    private final SemesterService semesterService;

    @GetMapping
    public ResponseEntity<List<SemesterResponse>> getAll() {
        return ResponseEntity.ok(semesterService.getAll());
    }

    @GetMapping("/current")
    public ResponseEntity<SemesterResponse> getCurrent() {
        return ResponseEntity.ok(semesterService.getCurrent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SemesterResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(semesterService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Créer un semestre - ADMIN_SCHOLAR")
    public ResponseEntity<SemesterResponse> create(@Valid @RequestBody CreateSemesterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(semesterService.create(request));
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "Clôturer un semestre - ADMIN_SCHOLAR")
    public ResponseEntity<SemesterResponse> close(@PathVariable Long id) {
        return ResponseEntity.ok(semesterService.close(id));
    }

    @PostMapping("/{id}/compute-progress")
    @Operation(summary = "Calculer les progressions - ADMIN_SCHOLAR")
    public ResponseEntity<Void> computeProgress(@PathVariable Long id) {
        semesterService.computeProgress(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/validate")
    @Operation(summary = "Valider le semestre - SUPER_ADMIN")
    public ResponseEntity<SemesterResponse> validate(@PathVariable Long id) {
        return ResponseEntity.ok(semesterService.validate(id));
    }
}
