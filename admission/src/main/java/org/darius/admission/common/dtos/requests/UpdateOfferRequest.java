package org.darius.admission.common.dtos.requests;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOfferRequest {
    private LocalDate deadline;
    private Integer maxCapacity;
}