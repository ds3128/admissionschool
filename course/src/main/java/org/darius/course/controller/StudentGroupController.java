package org.darius.course.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.course.dtos.requests.*;
import org.darius.course.dtos.responses.StudentGroupResponse;
import org.darius.course.services.StudentGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/courses/groups")
@RequiredArgsConstructor @Tag(name = "Groupes d'étudiants")
public class StudentGroupController {
    private final StudentGroupService groupService;

    @GetMapping
    public ResponseEntity<List<StudentGroupResponse>> getBySemester(@RequestParam Long semesterId) {
        return ResponseEntity.ok(groupService.getBySemester(semesterId));
    }
    @GetMapping("/{id}")
    public ResponseEntity<StudentGroupResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getById(id));
    }
    @GetMapping("/{id}/students")
    public ResponseEntity<List<String>> getStudents(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getStudentIds(id));
    }
    @PostMapping
    public ResponseEntity<StudentGroupResponse> create(
            @Valid @RequestBody CreateStudentGroupRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.create(req));
    }
    @PutMapping("/{id}/students")
    public ResponseEntity<StudentGroupResponse> updateStudents(
            @PathVariable Long id, @Valid @RequestBody UpdateGroupStudentsRequest req
    ) {
        return ResponseEntity.ok(groupService.updateStudents(id, req));
    }
}