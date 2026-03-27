package org.darius.course.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.course.dtos.requests.CreateTeacherAssignmentRequest;
import org.darius.course.dtos.responses.*;
import org.darius.course.services.TeacherAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/courses/assignments")
@RequiredArgsConstructor @Tag(name = "Affectations enseignants")
public class TeacherAssignmentController {
    private final TeacherAssignmentService assignmentService;

    @GetMapping
    public ResponseEntity<List<TeacherAssignmentResponse>> get(
            @RequestParam String teacherId,
            @RequestParam Long semesterId
    ) {
        return ResponseEntity.ok(
                assignmentService.getByTeacherAndSemester(teacherId, semesterId)
        );
    }
    @PostMapping
    public ResponseEntity<TeacherAssignmentResponse> create(
            @Valid @RequestBody CreateTeacherAssignmentRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.create(req));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assignmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/teachers/{teacherId}/load")
    public ResponseEntity<TeacherLoadResponse> getLoad(
            @PathVariable String teacherId,
            @RequestParam Long semesterId
    ) {
        return ResponseEntity.ok(assignmentService.getTeacherLoad(teacherId, semesterId));
    }
}
