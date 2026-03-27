package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.ConfirmationStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmationResponse {
    private Long id;
    private List<Long> acceptedChoiceIds;
    private LocalDateTime expiresAt;
    private Long confirmedChoiceId;
    private ConfirmationStatus status;
    private boolean autoConfirmed;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    // Délai restant en heures (calculé à la volée)
    private Long remainingHours;
}