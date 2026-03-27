package org.darius.notification.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_preferences")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unicité : une préférence par utilisateur
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    // Canal email activé (défaut true)
    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private boolean emailEnabled = true;

    // SMS activé (défaut false — v2)
    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private boolean smsEnabled = false;

    // Notifications du processus d'admission
    @Column(name = "admission_notifications", nullable = false)
    @Builder.Default
    private boolean admissionNotifications = true;

    // Notifications financières (factures, paiements)
    @Column(name = "payment_notifications", nullable = false)
    @Builder.Default
    private boolean paymentNotifications = true;

    // Notifications académiques générales (inscriptions, séances)
    @Column(name = "course_notifications", nullable = false)
    @Builder.Default
    private boolean courseNotifications = true;

    // Notifications de publication de notes
    @Column(name = "grade_notifications", nullable = false)
    @Builder.Default
    private boolean gradeNotifications = true;

    // Notifications de seuil d'absences
    @Column(name = "attendance_notifications", nullable = false)
    @Builder.Default
    private boolean attendanceNotifications = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
