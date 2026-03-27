package org.darius.notification.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.notification.dtos.responses.*;
import org.darius.notification.entities.Notification;
import org.darius.notification.entities.NotificationPreference;
import org.darius.notification.enums.*;
import org.darius.notification.exceptions.*;
import org.darius.notification.mail.MailService;
import org.darius.notification.repositories.NotificationRepository;
import org.darius.notification.services.NotificationService;
import org.darius.notification.services.PreferenceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final PreferenceService      preferenceService;
    private final MailService            mailService;

    // Notifications non désactivables — toujours envoyées
    private static final Set<NotificationType> CRITICAL_TYPES = Set.of(
            NotificationType.INVOICE_OVERDUE,
            NotificationType.STUDENT_PAYMENT_BLOCKED,
            NotificationType.APPLICATION_ACCEPTED
    );

    // ── Envoi standard ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void send(
            String userId,
            String recipientEmail,
            NotificationType type,
            String subject,
            String templateName,
            Map<String, Object> templateData,
            String referenceId,
            String referenceType
    ) {
        // Vérifier les préférences si userId disponible
        if (userId != null && !isAllowed(userId, type)) {
            log.debug("Notification {} ignorée — préférences userId={}", type, userId);
            return;
        }
        doSend(userId, recipientEmail, type, subject,
                templateName, templateData, referenceId, referenceType);
    }

    // ── Envoi critique — bypass préférences ───────────────────────────────────

    @Override
    @Transactional
    public void sendCritical(
            String userId,
            String recipientEmail,
            NotificationType type,
            String subject,
            String templateName,
            Map<String, Object> templateData,
            String referenceId,
            String referenceType
    ) {
        // Pas de vérification des préférences
        doSend(userId, recipientEmail, type, subject,
                templateName, templateData, referenceId, referenceType);
    }

    // ── Envoi interne ─────────────────────────────────────────────────────────

    private void doSend(
            String userId,
            String recipientEmail,
            NotificationType type,
            String subject,
            String templateName,
            Map<String, Object> templateData,
            String referenceId,
            String referenceType
    ) {
        // Créer l'entrée en base avec statut PENDING
        Notification notification = Notification.builder()
                .userId(userId)
                .recipientEmail(recipientEmail)
                .type(type)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .subject(subject)
                .body(templateName)   // sera remplacé après rendu
                .referenceId(referenceId)
                .referenceType(referenceType)
                .retryCount(0)
                .build();

        notification = notificationRepo.save(notification);

        // Rendu + envoi
        try {
            String html = mailService.renderTemplate(templateName, templateData);
            mailService.send(recipientEmail, subject, templateName, templateData);

            notification.setBody(html);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            log.info("Notification {} envoyée → {}", type, recipientEmail);

        } catch (Exception ex) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
            log.error("Échec envoi {} → {} : {}", type, recipientEmail, ex.getMessage());
        }

        notificationRepo.save(notification);
    }

    // ── Retry scheduler ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public void retryFailed() {
        List<Notification> toRetry = notificationRepo.findFailedForRetry();
        if (toRetry.isEmpty()) return;

        log.info("Retry : {} notification(s) à relancer", toRetry.size());

        for (Notification notification : toRetry) {
            notification.setStatus(NotificationStatus.RETRYING);
            notification.setRetryCount(notification.getRetryCount() + 1);
            notificationRepo.save(notification);

            try {
                // Le body contient le nom du template ou le HTML rendu
                String body = notification.getBody();
                if (!body.trim().startsWith("<")) {
                    // C'est un nom de template → re-render avec metadata vide
                    body = mailService.renderTemplate(body, Map.of());
                }
                mailService.send(
                        notification.getRecipientEmail(),
                        notification.getSubject(),
                        notification.getBody(),
                        Map.of()
                );

                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                log.info("Retry succès → {}", notification.getRecipientEmail());

            } catch (Exception ex) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage(ex.getMessage());

                if (notification.getRetryCount() >= 3) {
                    log.warn("Notification {} abandonnée après 3 tentatives — {}",
                            notification.getId(), notification.getRecipientEmail());
                }
            }
            notificationRepo.save(notification);
        }
    }

    // ── Requêtes utilisateur ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(
            String userId, int page, int size, NotificationType type
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notification> result = type != null
                ? notificationRepo.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable)
                : notificationRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return buildPage(result, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyUnread(String userId) {
        return notificationRepo
                .findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        return notificationRepo.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long id, String userId) {
        Notification notification = notificationRepo
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification introuvable : id=" + id
                ));
        notification.setReadAt(LocalDateTime.now());
        return toResponse(notificationRepo.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepo.markAllAsRead(userId, LocalDateTime.now());
    }

    // ── Administration ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getAll(
            int page, int size,
            String userId, NotificationType type, NotificationStatus status
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> result = notificationRepo.findAllByOrderByCreatedAtDesc(pageable);
        return buildPage(result, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationStatsResponse getStats() {
        long total    = notificationRepo.count();
        long sent     = notificationRepo.countByStatus(NotificationStatus.SENT);
        long failed   = notificationRepo.countByStatus(NotificationStatus.FAILED);
        long pending  = notificationRepo.countByStatus(NotificationStatus.PENDING);
        long retrying = notificationRepo.countByStatus(NotificationStatus.RETRYING);

        double successRate = total > 0
                ? Math.round((double) sent / total * 10000.0) / 100.0
                : 0.0;

        return NotificationStatsResponse.builder()
                .total(total)
                .sent(sent)
                .failed(failed)
                .pending(pending)
                .retrying(retrying)
                .successRate(successRate)
                .build();
    }

    @Override
    @Transactional
    public NotificationResponse forceResend(Long id) {
        Notification notification = notificationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification introuvable : id=" + id
                ));
        notification.setRetryCount(0);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setErrorMessage(null);
        return toResponse(notificationRepo.save(notification));
    }

    // ── Vérification préférences ──────────────────────────────────────────────

    private boolean isAllowed(String userId, NotificationType type) {
        // Les critiques passent toujours
        if (CRITICAL_TYPES.contains(type)) return true;

        NotificationPreference prefs = preferenceService.findOrCreate(userId);

        if (!prefs.isEmailEnabled()) return false;

        return switch (getCategory(type)) {
            case "ADMISSION"   -> prefs.isAdmissionNotifications();
            case "PAYMENT"     -> prefs.isPaymentNotifications();
            case "COURSE"      -> prefs.isCourseNotifications();
            case "GRADE"       -> prefs.isGradeNotifications();
            case "ATTENDANCE"  -> prefs.isAttendanceNotifications();
            default            -> true;
        };
    }

    private String getCategory(NotificationType type) {
        return switch (type) {
            case APPLICATION_SUBMITTED,
                 APPLICATION_ADMIN_REVIEW,
                 APPLICATION_PENDING_COMMISSION,
                 INTERVIEW_SCHEDULED,
                 THESIS_APPROVAL_REQUESTED,
                 APPLICATION_AWAITING_CONFIRMATION,
                 APPLICATION_ACCEPTED,
                 APPLICATION_REJECTED,
                 WAITLIST_PROMOTED,
                 CHOICE_AUTO_CONFIRMED      -> "ADMISSION";

            case PAYMENT_COMPLETED,
                 PAYMENT_FAILED,
                 PAYMENT_REFUNDED,
                 INVOICE_GENERATED,
                 INVOICE_PAID,
                 INVOICE_OVERDUE,
                 STUDENT_PAYMENT_BLOCKED,
                 SCHOLARSHIP_DISBURSED      -> "PAYMENT";

            case STUDENT_ENROLLED,
                 SESSION_CANCELLED,
                 SEMESTER_VALIDATED         -> "COURSE";

            case GRADES_PUBLISHED           -> "GRADE";

            case ATTENDANCE_THRESHOLD_EXCEEDED -> "ATTENDANCE";
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PageResponse<NotificationResponse> buildPage(
            Page<Notification> page, int pageNum, int size
    ) {
        return PageResponse.<NotificationResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(pageNum)
                .size(size)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .recipientEmail(n.getRecipientEmail())
                .type(n.getType())
                .channel(n.getChannel())
                .status(n.getStatus())
                .subject(n.getSubject())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .retryCount(n.getRetryCount())
                .sentAt(n.getSentAt())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .read(n.getReadAt() != null)
                .build();
    }
}
