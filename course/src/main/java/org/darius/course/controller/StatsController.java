package org.darius.course.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.darius.course.dtos.responses.*;
import org.darius.course.services.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/courses/ml")
@RequiredArgsConstructor
@Tag(name = "Pipeline ML - Données statistiques")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/attendance-stats")
    public ResponseEntity<List<AttendanceStatsResponse>> getAttendanceStats(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long filiereId
    ) {
        return ResponseEntity.ok(statsService.getAttendanceStatsForML(semesterId, filiereId));
    }

    @GetMapping("/grade-stats")
    public ResponseEntity<List<ClassStatsResponse>> getGradeStats(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long matiereId
    ) {
        return ResponseEntity.ok(statsService.getGradeStatsForML(semesterId, matiereId));
    }

    @GetMapping("/progress-summary")
    public ResponseEntity<Map<String, Object>> getProgressSummary(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long groupId
    ) {
        return ResponseEntity.ok(statsService.getProgressSummaryForML(semesterId, groupId));
    }
}
