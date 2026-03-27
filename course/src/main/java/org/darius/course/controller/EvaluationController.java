package org.darius.course.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.course.dtos.requests.*;
import org.darius.course.dtos.responses.*;
import org.darius.course.services.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/courses/evaluations")
@RequiredArgsConstructor
@Tag(name = "Évaluations et notes")
public class EvaluationController {

    private final EvaluationService           evaluationService;
    private final GradeService                gradeService;
    private final EvaluationAttachmentService attachmentService;

    @GetMapping
    public ResponseEntity<List<EvaluationResponse>> getByMatiereAndSemester(
            @RequestParam Long matiereId,
            @RequestParam Long semesterId
    ) {
        return ResponseEntity.ok(evaluationService.getByMatiereAndSemester(matiereId, semesterId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvaluationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationService.getById(id));
    }

    @PostMapping
    public ResponseEntity<EvaluationResponse> create(
            @RequestHeader("X-User-Id") String teacherId,
            @Valid @RequestBody CreateEvaluationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evaluationService.create(teacherId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvaluationResponse> update(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String teacherId,
            @Valid @RequestBody CreateEvaluationRequest request
    ) {
        return ResponseEntity.ok(evaluationService.update(id, teacherId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String teacherId
    ) {
        evaluationService.delete(id, teacherId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<EvaluationResponse> publish(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String teacherId
    ) {
        return ResponseEntity.ok(evaluationService.publish(id, teacherId));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ClassStatsResponse> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationService.getStats(id));
    }

    // ── Notes ─────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/grades")
    public ResponseEntity<List<GradeResponse>> submitGrades(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String teacherId,
            @Valid @RequestBody SubmitGradesRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gradeService.submitGrades(id, teacherId, request));
    }

    @PutMapping("/{id}/grades/{gradeId}")
    public ResponseEntity<GradeResponse> updateGrade(
            @PathVariable Long id,
            @PathVariable Long gradeId,
            @RequestHeader("X-User-Id") String teacherId,
            @RequestParam double score,
            @RequestParam(required = false) String comment
    ) {
        return ResponseEntity.ok(gradeService.updateGrade(gradeId, teacherId, score, comment));
    }

    // ── Pièces jointes ────────────────────────────────────────────────────────

    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<EvaluationAttachmentResponse>> getAttachments(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(attachmentService.getByEvaluation(id));
    }

    @PostMapping("/{id}/attachments")
    public ResponseEntity<EvaluationAttachmentResponse> addAttachment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String teacherId,
            @RequestPart("data") @Valid CreateAttachmentRequest request,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.addAttachment(id, teacherId, request, file));
    }

    @DeleteMapping("/{id}/attachments/{attachId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long id,
            @PathVariable Long attachId,
            @RequestHeader("X-User-Id") String teacherId
    ) {
        attachmentService.delete(attachId, teacherId);
        return ResponseEntity.noContent().build();
    }
}
