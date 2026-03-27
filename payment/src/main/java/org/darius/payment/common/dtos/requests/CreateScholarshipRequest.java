package org.darius.payment.common.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.darius.payment.common.enums.DisbursementFrequency;
import org.darius.payment.common.enums.ScholarshipSource;
import org.darius.payment.common.enums.ScholarshipType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateScholarshipRequest {

    @NotBlank
    private String studentId;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{4}")
    private String academicYear;

    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    private DisbursementFrequency disbursementFrequency;

    @NotNull
    private ScholarshipType type;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private String conditions;

    // Obligatoire pour MERITE — défaut 14.0
    @DecimalMin("0.0") @DecimalMax("20.0")
    private BigDecimal minimumGrade;

    @NotNull(message = "La source de la bourse est obligatoire")
    @Builder.Default
    private ScholarshipSource source = ScholarshipSource.INSTITUTIONNELLE;
}