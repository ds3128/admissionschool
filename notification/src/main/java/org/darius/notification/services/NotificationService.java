package org.darius.notification.services;

import org.darius.notification.dtos.responses.*;
import org.darius.notification.enums.NotificationStatus;
import org.darius.notification.enums.NotificationType;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    /**
     * Envoie une notification en vérifiant les préférences utilisateur.
     */
    void send(
            String userId,
            String recipientEmail,
            NotificationType type,
            String subject,
            String templateName,
            Map<String, Object> templateData,
            String referenceId,
            String referenceType
    );

    /**
     * Envoie une notification critique — bypass des préférences.
     * Utilisé pour INVOICE_OVERDUE, STUDENT_PAYMENT_BLOCKED, APPLICATION_ACCEPTED.
     */
    void sendCritical(
            String userId,
            String recipientEmail,
            NotificationType type,
            String subject,
            String templateName,
            Map<String, Object> templateData,
            String referenceId,
            String referenceType
    );

    /**
     * Renvoi des notifications en échec (retryCount < 3).
     * Appelé par le scheduler toutes les 5 minutes.
     */
    void retryFailed();

    // ── Requêtes utilisateur ──────────────────────────────────────────────────

    PageResponse<NotificationResponse> getMyNotifications(
            String userId, int page, int size, NotificationType type
    );

    List<NotificationResponse> getMyUnread(String userId);

    long countUnread(String userId);

    NotificationResponse markAsRead(Long id, String userId);

    void markAllAsRead(String userId);

    // ── Administration ────────────────────────────────────────────────────────

    PageResponse<NotificationResponse> getAll(
            int page, int size, String userId,
            NotificationType type, NotificationStatus status
    );

    NotificationStatsResponse getStats();

    NotificationResponse forceResend(Long id);
}
