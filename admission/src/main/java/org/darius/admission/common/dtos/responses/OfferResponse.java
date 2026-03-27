package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.OfferLevel;
import org.darius.admission.common.enums.OfferStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferResponse {
    private Long id;
    private Long campaignId;
    private String academicYear;
    private Long filiereId;
    private String filiereName;
    private OfferLevel level;
    private LocalDate deadline;
    private int maxCapacity;
    private int currentCount;
    private int acceptedCount;
    private int waitlistCount;
    private int availablePlaces;  // maxCapacity - acceptedCount
    private OfferStatus status;
    private List<RequiredDocumentResponse> requiredDocuments;
    private LocalDateTime createdAt;
}