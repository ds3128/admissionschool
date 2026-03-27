package org.darius.course.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.course.dtos.requests.UpdateCourseResourceRequest;
import org.darius.course.dtos.responses.CourseResourceResponse;
import org.darius.course.services.CourseResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses/resources")
@RequiredArgsConstructor
@Tag(name = "Supports de cours")
public class CourseResourceController {

    private final CourseResourceService resourceService;

    @GetMapping("/my")
    public ResponseEntity<List<CourseResourceResponse>> getMyResources(
            @RequestHeader("X-User-Id") String teacherId
    ) {
        return ResponseEntity.ok(resourceService.getMyResources(teacherId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResourceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResourceResponse> update(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String teacherId,
            @Valid @RequestBody UpdateCourseResourceRequest request
    ) {
        return ResponseEntity.ok(resourceService.update(id, teacherId, request));
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<CourseResourceResponse> publish(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String teacherId
    ) {
        return ResponseEntity.ok(resourceService.publish(id, teacherId));
    }

    @PutMapping("/{id}/unpublish")
    public ResponseEntity<CourseResourceResponse> unpublish(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String teacherId
    ) {
        return ResponseEntity.ok(resourceService.unpublish(id, teacherId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String teacherId
    ) {
        resourceService.delete(id, teacherId);
        return ResponseEntity.noContent().build();
    }
}
