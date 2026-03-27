package org.darius.notification.dtos.responses;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PreferenceResponse {
    private Long          id;
    private String        userId;
    private boolean       emailEnabled;
    private boolean       smsEnabled;
    private boolean       admissionNotifications;
    private boolean       paymentNotifications;
    private boolean       courseNotifications;
    private boolean       gradeNotifications;
    private boolean       attendanceNotifications;
    private LocalDateTime updatedAt;
}
