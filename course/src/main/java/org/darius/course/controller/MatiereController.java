package org.darius.course.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.course.dtos.requests.*;
import org.darius.course.dtos.responses.*;
import org.darius.course.services.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/courses/matieres")
@RequiredArgsConstructor
@Tag(name = "Matières")
public class MatiereController {

    private final MatiereService         matiereService;
    private final CourseResourceService  resourceService;

    @GetMapping
    public ResponseEntity<List<MatiereResponse>> getAll(
            @RequestParam(required = false) Long teachingUnitId
    ) {
        return ResponseEntity.ok(teachingUnitId != null
                ? matiereService.getByTeachingUnit(teachingUnitId)
                : matiereService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatiereResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(matiereService.getById(id));
    }

    @PostMapping
    public ResponseEntity<MatiereResponse> create(@Valid @RequestBody CreateMatiereRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matiereService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatiereResponse> update(
            @PathVariable Long id, @Valid @RequestBody CreateMatiereRequest req
    ) {
        return ResponseEntity.ok(matiereService.update(id, req));
    }

    // ── Supports de cours ─────────────────────────────────────────────────────

    @GetMapping("/{id}/resources")
    public ResponseEntity<List<CourseResourceResponse>> getResources(
            @PathVariable Long id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role
    ) {
        // Enseignant voit ses propres supports (publiés + brouillons)
        if ("TEACHER".equalsIgnoreCase(role)) {
            return ResponseEntity.ok(resourceService.getMyResources(userId).stream()
                    .filter(r -> r.getMatiereId().equals(id))
                    .toList());
        }
        // Étudiant voit uniquement les supports publiés
        return ResponseEntity.ok(resourceService.getPublishedByMatiere(id, userId));
    }

    @PostMapping("/{id}/resources")
    public ResponseEntity<CourseResourceResponse> addResource(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String teacherId,
            @RequestPart("data") @Valid CreateCourseResourceRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.addResource(id, teacherId, request, file));
    }
}
