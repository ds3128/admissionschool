package org.darius.notification.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.darius.notification.dtos.responses.*;
import org.darius.notification.enums.NotificationStatus;
import org.darius.notification.enums.NotificationType;
import org.darius.notification.services.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    // ── Utilisateur ───────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Mes notifications paginées")
    public ResponseEntity<PageResponse<NotificationResponse>> getMyNotifications(
            @RequestHeader("X-User-Id")   String userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    NotificationType type
    ) {
        return ResponseEntity.ok(
                notificationService.getMyNotifications(userId, page, size, type)
        );
    }

    @GetMapping("/me/unread")
    @Operation(summary = "Mes notifications non lues")
    public ResponseEntity<List<NotificationResponse>> getMyUnread(
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(notificationService.getMyUnread(userId));
    }

    @GetMapping("/me/count")
    @Operation(summary = "Nombre de notifications non lues")
    public ResponseEntity<UnreadCountResponse> countUnread(
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(
                UnreadCountResponse.builder()
                        .count(notificationService.countUnread(userId))
                        .build()
        );
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable           Long   id,
            @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }

    @PutMapping("/me/read-all")
    @Operation(summary = "Tout marquer comme lu")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader("X-User-Id") String userId
    ) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    // ── Administration ────────────────────────────────────────────────────────

    @GetMapping("/admin")
    @Operation(summary = "Toutes les notifications — ADMIN")
    public ResponseEntity<PageResponse<NotificationResponse>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String userId,
            @RequestParam(required = false)    NotificationType type,
            @RequestParam(required = false)    NotificationStatus status
    ) {
        return ResponseEntity.ok(
                notificationService.getAll(page, size, userId, type, status)
        );
    }

    @GetMapping("/admin/stats")
    @Operation(summary = "Statistiques d'envoi — ADMIN")
    public ResponseEntity<NotificationStatsResponse> getStats() {
        return ResponseEntity.ok(notificationService.getStats());
    }

    @PostMapping("/admin/{id}/resend")
    @Operation(summary = "Forcer le renvoi d'une notification — ADMIN")
    public ResponseEntity<NotificationResponse> forceResend(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.forceResend(id));
    }
}
