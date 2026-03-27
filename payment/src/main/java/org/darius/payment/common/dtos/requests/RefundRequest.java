package org.darius.payment.common.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundRequest {

    @NotBlank(message = "Le motif de remboursement est obligatoire")
    private String reason;
}