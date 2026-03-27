package org.darius.course.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.course.dtos.requests.CreateTeachingUnitRequest;
import org.darius.course.dtos.responses.TeachingUnitResponse;
import org.darius.course.services.TeachingUnitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/courses/teaching-units")
@RequiredArgsConstructor @Tag(name = "Unités d'enseignement")
public class TeachingUnitController {
    private final TeachingUnitService service;

    @GetMapping
    public ResponseEntity<List<TeachingUnitResponse>> getAll(
            @RequestParam(required = false) Long levelId
    ) {
        return ResponseEntity.ok(levelId != null
                ? service.getByStudyLevel(levelId) : service.getAll());
    }
    @GetMapping("/{id}")
    public ResponseEntity<TeachingUnitResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }
    @PostMapping
    public ResponseEntity<TeachingUnitResponse> create(
            @Valid @RequestBody CreateTeachingUnitRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }
    @PutMapping("/{id}")
    public ResponseEntity<TeachingUnitResponse> update(
            @PathVariable Long id, @Valid @RequestBody CreateTeachingUnitRequest req
    ) {
        return ResponseEntity.ok(service.update(id, req));
    }
}
