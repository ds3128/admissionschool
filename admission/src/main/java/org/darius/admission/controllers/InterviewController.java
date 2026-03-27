package org.darius.admission.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.admission.common.dtos.requests.CompleteInterviewRequest;
import org.darius.admission.common.dtos.requests.ScheduleInterviewRequest;
import org.darius.admission.common.dtos.responses.InterviewResponse;
import org.darius.admission.services.InterviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Entretiens", description = "Planification et suivi des entretiens")
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/admissions/applications/{applicationId}/choices/{choiceId}/interview")
    @Operation(summary = "Planifier un entretien — ADMIN_SCHOLAR")
    public ResponseEntity<InterviewResponse> schedule(
            @PathVariable String applicationId,
            @PathVariable Long choiceId,
            @Valid @RequestBody ScheduleInterviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewService.scheduleInterview(applicationId, choiceId, request));
    }

    @GetMapping("/admissions/interviews/{id}")
    @Operation(summary = "Détail d'un entretien")
    public ResponseEntity<InterviewResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(interviewService.getInterviewById(id));
    }

    @PutMapping("/admissions/interviews/{id}/complete")
    @Operation(summary = "Clôturer un entretien — TEACHER (président)")
    public ResponseEntity<InterviewResponse> complete(
            @PathVariable Long id,
            @RequestBody CompleteInterviewRequest request
    ) {
        return ResponseEntity.ok(interviewService.completeInterview(id, request));
    }

    @PutMapping("/admissions/interviews/{id}/cancel")
    @Operation(summary = "Annuler un entretien — ADMIN_SCHOLAR")
    public ResponseEntity<InterviewResponse> cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(interviewService.cancelInterview(id, reason));
    }
}