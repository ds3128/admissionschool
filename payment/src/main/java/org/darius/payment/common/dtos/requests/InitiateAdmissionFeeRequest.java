package org.darius.payment.common.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.darius.payment.common.enums.PaymentMethod;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InitiateAdmissionFeeRequest {

    @NotBlank(message = "L'identifiant de la candidature est obligatoire")
    private String applicationId;

    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    @Builder.Default
    private String currency = "XAF";

    @NotNull(message = "La méthode de paiement est obligatoire")
    private PaymentMethod method;
}
