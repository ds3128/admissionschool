package org.darius.course.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.course.dtos.requests.CreatePlannedSlotRequest;
import org.darius.course.dtos.responses.*;
import org.darius.course.services.PlannedSlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Emploi du temps")
public class PlannedSlotController {

    private final PlannedSlotService slotService;

    @GetMapping("/slots")
    public ResponseEntity<List<PlannedSlotResponse>> getSlots(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long groupId
    ) {
        return ResponseEntity.ok(groupId != null
                ? slotService.getByGroupAndSemester(groupId, semesterId)
                : slotService.getBySemester(semesterId));
    }

    @PostMapping("/slots")
    public ResponseEntity<PlannedSlotResponse> create(
            @Valid @RequestBody CreatePlannedSlotRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slotService.create(request));
    }

    @PutMapping("/slots/{id}")
    public ResponseEntity<PlannedSlotResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreatePlannedSlotRequest request
    ) {
        return ResponseEntity.ok(slotService.update(id, request));
    }

    @DeleteMapping("/slots/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        slotService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/schedule")
    public ResponseEntity<WeeklyScheduleResponse> getSchedule(
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week
    ) {
        LocalDate weekDate = week != null ? week : LocalDate.now();
        return ResponseEntity.ok(slotService.getWeeklySchedule(userId, role, weekDate));
    }
}
