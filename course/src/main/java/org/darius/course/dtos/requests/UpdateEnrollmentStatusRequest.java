package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.darius.course.enums.EnrollStatus;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateEnrollmentStatusRequest {

    @NotNull
    private EnrollStatus status;

    private String reason;
}