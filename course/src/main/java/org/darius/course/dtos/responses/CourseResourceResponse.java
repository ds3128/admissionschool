package org.darius.course.dtos.responses;

import lombok.*;
import org.darius.course.enums.ResourceType;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseResourceResponse {
    private Long id;
    private Long matiereId;
    private String matiereName;
    private Long semesterId;
    private String title;
    private String description;
    private ResourceType type;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private boolean isPublished;
    private String uploadedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}