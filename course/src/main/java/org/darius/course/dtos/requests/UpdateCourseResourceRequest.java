package org.darius.course.dtos.requests;

import lombok.*;
import org.darius.course.enums.ResourceType;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateCourseResourceRequest {
    private String title;
    private String description;
    private ResourceType type;
}

