package org.darius.notification.repositories;

import org.darius.notification.entities.Notification;
import org.darius.notification.enums.NotificationStatus;
import org.darius.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ── Requêtes utilisateur ──────────────────────────────────────────────────

    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(
            String userId, NotificationType type, Pageable pageable
    );

    List<Notification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(String userId);

    long countByUserIdAndReadAtIsNull(String userId);

    Optional<Notification> findByIdAndUserId(Long id, String userId);

    // ── Marquer toutes comme lues ─────────────────────────────────────────────

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.readAt = :now
        WHERE n.userId = :userId AND n.readAt IS NULL
        """)
    void markAllAsRead(@Param("userId") String userId, @Param("now") LocalDateTime now);

    // ── Retry scheduler ───────────────────────────────────────────────────────

    @Query("""
        SELECT n FROM Notification n
        WHERE n.status = 'FAILED'
          AND n.retryCount < 3
        ORDER BY n.createdAt ASC
        """)
    List<Notification> findFailedForRetry();

    // ── Administration ────────────────────────────────────────────────────────

    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(
            String userId, Pageable pageable,
            @Param("ignored") Object ignored // workaround overload
    );

    // Stats
    long countByStatus(NotificationStatus status);

    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.status = 'SENT'
          AND n.sentAt >= :since
        """)
    long countSentSince(@Param("since") LocalDateTime since);
}
