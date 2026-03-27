package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class
ApplicationResponse {
    private String id;
    private String userId;
    private Long campaignId;
    private String academicYear;
    private ApplicationStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime paidAt;
    private LocalDateTime lastStatusChange;
    private List<ChoiceResponse> choices;
    private DossierResponse dossier;
    private CandidateProfileResponse candidateProfile;
    private PaymentResponse payment;
    private ConfirmationResponse confirmationRequest;
    private List<StatusHistoryResponse> statusHistory;
    private LocalDateTime createdAt;
}