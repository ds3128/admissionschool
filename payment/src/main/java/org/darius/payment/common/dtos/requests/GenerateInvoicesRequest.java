package org.darius.payment.common.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GenerateInvoicesRequest {

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{4}", message = "Format attendu : 2026-2027")
    private String academicYear;

    @NotBlank
    @Pattern(regexp = "S1|S2|ANNUEL", message = "Valeurs acceptées : S1, S2, ANNUEL")
    private String semester;

    @NotNull
    private LocalDate dueDate;

    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;
}
