package org.darius.notification.dtos.responses;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationStatsResponse {
    private long   total;
    private long   sent;
    private long   failed;
    private long   pending;
    private long   retrying;
    private double successRate;
}
