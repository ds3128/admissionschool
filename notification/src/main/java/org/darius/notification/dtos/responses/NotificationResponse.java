package org.darius.notification.dtos.responses;

import lombok.*;
import org.darius.notification.enums.NotificationChannel;
import org.darius.notification.enums.NotificationStatus;
import org.darius.notification.enums.NotificationType;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResponse {
    private Long                id;
    private String              userId;
    private String              recipientEmail;
    private NotificationType    type;
    private NotificationChannel channel;
    private NotificationStatus  status;
    private String              subject;
    private String              referenceId;
    private String              referenceType;
    private int                 retryCount;
    private LocalDateTime       sentAt;
    private LocalDateTime       readAt;
    private LocalDateTime       createdAt;
    private boolean             read;
}
