package org.darius.payment.common.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateScheduleRequest {

    @NotEmpty(message = "Au moins une échéance est requise")
    private List<InstallmentRequest> installments;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class InstallmentRequest {
        @NotNull @DecimalMin("0.01")
        private BigDecimal amount;
        @NotNull
        private LocalDate dueDate;
    }
}