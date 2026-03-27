package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.OfferLevel;
import org.darius.admission.common.enums.OfferStatus;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferSummaryResponse {
    private Long id;
    private Long filiereId;
    private String filiereName;
    private OfferLevel level;
    private LocalDate deadline;
    private int availablePlaces;
    private OfferStatus status;
}