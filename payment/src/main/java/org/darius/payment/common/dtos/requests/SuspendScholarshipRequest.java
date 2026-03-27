package org.darius.payment.common.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SuspendScholarshipRequest {

    @NotBlank(message = "Le motif de suspension est obligatoire")
    private String reason;
}