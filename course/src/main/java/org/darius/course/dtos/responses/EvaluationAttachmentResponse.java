package org.darius.course.dtos.responses;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EvaluationAttachmentResponse {
    private Long id;
    private Long evaluationId;
    private String title;
    private String description;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String uploadedBy;
    private LocalDateTime createdAt;
}