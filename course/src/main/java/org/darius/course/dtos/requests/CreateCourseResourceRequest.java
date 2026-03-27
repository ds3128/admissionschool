package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.darius.course.enums.ResourceType;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateCourseResourceRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private ResourceType type;

    @NotNull
    private Long semesterId;

    // Pour les liens externes — null si fichier uploadé
    private String externalUrl;
}