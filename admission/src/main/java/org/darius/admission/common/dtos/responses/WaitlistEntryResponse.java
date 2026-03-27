package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.WaitlistStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistEntryResponse {
    private Long id;
    private Long offerId;
    private int rank;
    private WaitlistStatus status;
    private LocalDateTime promotedAt;
    private LocalDateTime expiresAt;
    // Délai restant en heures si PROMOTED
    private Long remainingHours;
}