package org.darius.notification.entities;

import jakarta.persistence.*;
import lombok.*;
import org.darius.notification.enums.NotificationChannel;
import org.darius.notification.enums.NotificationStatus;
import org.darius.notification.enums.NotificationType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notif_user_id",    columnList = "user_id"),
                @Index(name = "idx_notif_status",      columnList = "status"),
                @Index(name = "idx_notif_type",        columnList = "type"),
                @Index(name = "idx_notif_created_at",  columnList = "created_at")
        }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Destinataire — référence Auth Service (null pour certains events sans userId)
    @Column(name = "user_id")
    private String userId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationChannel channel = NotificationChannel.EMAIL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(nullable = false)
    private String subject;

    // Corps HTML de la notification
    @Column(columnDefinition = "TEXT")
    private String body;

    // Données contextuelles sérialisées en JSON
    // ex: { "applicationId": "abc", "studentNumber": "2026-0001" }
    @Column(columnDefinition = "TEXT")
    private String metadata;

    // ID de l'entité source (applicationId, invoiceId, evaluationId...)
    @Column(name = "reference_id")
    private String referenceId;

    // Type de l'entité source (APPLICATION, INVOICE, EVALUATION...)
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // Date de lecture in-app — null = non lue
    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
