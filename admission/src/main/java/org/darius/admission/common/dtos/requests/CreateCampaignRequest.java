package org.darius.admission.common.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCampaignRequest {

    @NotBlank(message = "L'année académique est obligatoire")
    @Pattern(regexp = "\\d{4}-\\d{4}", message = "Format attendu : 2025-2026")
    private String academicYear;

    @NotNull(message = "La date d'ouverture est obligatoire")
    @FutureOrPresent(message = "La date d'ouverture doit être aujourd'hui ou dans le futur")
    private LocalDate startDate;

    @NotNull(message = "La date de clôture est obligatoire")
    private LocalDate endDate;

    private LocalDate resultsDate;

    @Min(value = 1, message = "Le délai de confirmation doit être d'au moins 1 jour")
    @Builder.Default
    private int confirmationDeadlineDays = 5;

    @NotNull(message = "Le montant des frais est obligatoire")
    @DecimalMin(value = "0.0", message = "Le montant doit être positif")
    private BigDecimal feeAmount;

    @Min(value = 1, message = "Au moins 1 choix")
    @Max(value = 3, message = "Maximum 3 choix")
    @Builder.Default
    private int maxChoicesPerApplication = 3;
}