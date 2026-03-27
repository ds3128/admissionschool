package org.darius.payment.common.dtos.requests;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebhookRequest {
    private String externalReference;
    private String status;     // "SUCCESS" ou "FAILED"
    private String failureReason;
}