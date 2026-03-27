package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateAttachmentRequest {

    @NotBlank
    private String title;

    private String description;
}