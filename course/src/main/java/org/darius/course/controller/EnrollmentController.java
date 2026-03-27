package org.darius.course.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.course.dtos.requests.UpdateEnrollmentStatusRequest;
import org.darius.course.dtos.responses.*;
import org.darius.course.services.EnrollmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses/enrollments")
@RequiredArgsConstructor
@Tag(name = "Inscriptions")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    public ResponseEntity<List<EnrollmentResponse>> getEnrollments(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) Long semesterId
    ) {
        if (studentId != null && semesterId != null) {
            return ResponseEntity.ok(
                    enrollmentService.getByStudentAndSemester(studentId, semesterId)
            );
        }
        return ResponseEntity.ok(List.of());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<EnrollmentResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEnrollmentStatusRequest request
    ) {
        return ResponseEntity.ok(enrollmentService.updateStatus(id, request));
    }
}
