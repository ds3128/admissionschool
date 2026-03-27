package org.darius.course.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.course.dtos.requests.*;
import org.darius.course.dtos.responses.*;
import org.darius.course.services.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses/sessions")
@RequiredArgsConstructor
@Tag(name = "Séances et présences")
public class SessionController {

    private final SessionService    sessionService;
    private final AttendanceService attendanceService;

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getByMatiereAndSemester(
            @RequestParam Long matiereId,
            @RequestParam Long semesterId
    ) {
        return ResponseEntity.ok(sessionService.getByMatiereAndSemester(matiereId, semesterId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getById(id));
    }

    @PutMapping("/{id}/attendance")
    public ResponseEntity<List<AttendanceResponse>> markAttendance(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String teacherId,
            @Valid @RequestBody MarkAttendanceRequest request
    ) {
        return ResponseEntity.ok(sessionService.markAttendance(id, teacherId, request));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<SessionResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelSessionRequest request
    ) {
        return ResponseEntity.ok(sessionService.cancel(id, request));
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<SessionResponse> reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleSessionRequest request
    ) {
        return ResponseEntity.ok(sessionService.reschedule(id, request));
    }
}
