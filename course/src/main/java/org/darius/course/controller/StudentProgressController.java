package org.darius.course.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.darius.course.dtos.responses.*;
import org.darius.course.services.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses/students")
@RequiredArgsConstructor
@Tag(name = "Progression étudiants")
public class StudentProgressController {

    private final StudentProgressService progressService;
    private final AttendanceService      attendanceService;
    private final EnrollmentService      enrollmentService;
    private final GradeService           gradeService;

    @GetMapping("/{id}/progress")
    public ResponseEntity<StudentProgressResponse> getProgress(
            @PathVariable String id,
            @RequestParam Long semesterId
    ) {
        return ResponseEntity.ok(progressService.getByStudentAndSemester(id, semesterId));
    }

    @GetMapping("/{id}/transcript")
    public ResponseEntity<TranscriptResponse> getTranscript(
            @PathVariable String id,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) Long semesterId
    ) {
        return ResponseEntity.ok(progressService.getTranscript(id, year, semesterId));
    }

    @GetMapping("/{id}/attendance-stats")
    public ResponseEntity<List<AttendanceStatsResponse>> getAttendanceStats(
            @PathVariable String id,
            @RequestParam Long semesterId
    ) {
        return ResponseEntity.ok(attendanceService.getStatsByStudentAndSemester(id, semesterId));
    }

    @GetMapping("/{id}/grades")
    public ResponseEntity<List<GradeResponse>> getGrades(
            @PathVariable String id,
            @RequestParam Long semesterId
    ) {
        return ResponseEntity.ok(gradeService.getByStudentAndSemester(id, semesterId));
    }

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<List<CourseDashboardEntry>> getDashboard(
            @PathVariable String id,
            @RequestParam Long semesterId
    ) {
        return ResponseEntity.ok(enrollmentService.getStudentDashboard(id, semesterId));
    }

    @GetMapping("/{id}/attendance")
    public ResponseEntity<List<AttendanceResponse>> getAttendance(
            @PathVariable String id,
            @RequestParam Long sessionId
    ) {
        return ResponseEntity.ok(attendanceService.getBySession(sessionId));
    }
}
