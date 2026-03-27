package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateGroupStudentsRequest {

    @NotEmpty
    private List<String> studentIds;

    // ADD ou REMOVE
    @NotBlank
    private String action;
}