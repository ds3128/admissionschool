package org.darius.course.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.course.dtos.requests.CreateRoomRequest;
import org.darius.course.dtos.responses.RoomResponse;
import org.darius.course.services.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@RestController @RequestMapping("/courses/rooms")
@RequiredArgsConstructor @Tag(name = "Salles")
public class RoomController {
    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAll() {
        return ResponseEntity.ok(roomService.getAll());
    }
    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getById(id));
    }
    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(req));
    }
    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> update(
            @PathVariable Long id, @Valid @RequestBody CreateRoomRequest req
    ) {
        return ResponseEntity.ok(roomService.update(id, req));
    }
    @GetMapping("/available")
    public ResponseEntity<List<RoomResponse>> getAvailable(
            @RequestParam DayOfWeek day,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime end
    ) {
        return ResponseEntity.ok(roomService.getAvailable(day, start, end));
    }
}