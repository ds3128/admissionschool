package org.darius.admission.common.dtos.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.darius.admission.common.enums.CampaignStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCampaignStatusRequest {

    @NotNull(message = "Le statut est obligatoire")
    private CampaignStatus status;

    private String reason;
}