package org.darius.course.dtos.responses;

import lombok.*;
import org.darius.course.enums.EvalType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EvaluationResponse {
    private Long id;
    private String title;
    private EvalType type;
    private Long matiereId;
    private String matiereName;
    private Long semesterId;
    private LocalDate date;
    private double coefficient;
    private double maxScore;
    private boolean isPublished;
    private List<EvaluationAttachmentResponse> attachments;
    private LocalDateTime createdAt;
}