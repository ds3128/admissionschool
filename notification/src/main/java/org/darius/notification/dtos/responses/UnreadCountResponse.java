package org.darius.notification.dtos.responses;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnreadCountResponse {
    private long count;
}
