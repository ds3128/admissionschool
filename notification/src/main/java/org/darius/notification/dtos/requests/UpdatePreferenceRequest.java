package org.darius.notification.dtos.requests;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdatePreferenceRequest {
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean admissionNotifications;
    private Boolean paymentNotifications;
    private Boolean courseNotifications;
    private Boolean gradeNotifications;
    private Boolean attendanceNotifications;
}
