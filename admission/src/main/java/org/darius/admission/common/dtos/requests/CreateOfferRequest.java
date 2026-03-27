package org.darius.admission.common.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.darius.admission.common.enums.OfferLevel;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOfferRequest {

    @NotNull(message = "La campagne est obligatoire")
    private Long campaignId;

    @NotNull(message = "La filière est obligatoire")
    private Long filiereId;

    @NotBlank(message = "Le nom de la filière est obligatoire")
    private String filiereName;

    @NotNull(message = "Le niveau est obligatoire")
    private OfferLevel level;

    @NotNull(message = "La date limite est obligatoire")
    private LocalDate deadline;

    @Min(value = 1, message = "La capacité doit être d'au moins 1")
    private int maxCapacity;
}