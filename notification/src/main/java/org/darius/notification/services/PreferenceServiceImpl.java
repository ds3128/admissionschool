package org.darius.notification.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.notification.dtos.requests.UpdatePreferenceRequest;
import org.darius.notification.dtos.responses.PreferenceResponse;
import org.darius.notification.entities.NotificationPreference;
import org.darius.notification.repositories.NotificationPreferenceRepository;
import org.darius.notification.services.PreferenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreferenceServiceImpl implements PreferenceService {

    private final NotificationPreferenceRepository prefRepo;

    @Override
    @Transactional
    public PreferenceResponse getPreferences(String userId) {
        return toResponse(findOrCreate(userId));
    }

    @Override
    @Transactional
    public PreferenceResponse updatePreferences(
            String userId, UpdatePreferenceRequest request
    ) {
        NotificationPreference pref = findOrCreate(userId);

        if (request.getEmailEnabled()            != null) pref.setEmailEnabled(request.getEmailEnabled());
        if (request.getSmsEnabled()              != null) pref.setSmsEnabled(request.getSmsEnabled());
        if (request.getAdmissionNotifications()  != null) pref.setAdmissionNotifications(request.getAdmissionNotifications());
        if (request.getPaymentNotifications()    != null) pref.setPaymentNotifications(request.getPaymentNotifications());
        if (request.getCourseNotifications()     != null) pref.setCourseNotifications(request.getCourseNotifications());
        if (request.getGradeNotifications()      != null) pref.setGradeNotifications(request.getGradeNotifications());
        if (request.getAttendanceNotifications() != null) pref.setAttendanceNotifications(request.getAttendanceNotifications());

        return toResponse(prefRepo.save(pref));
    }

    @Override
    @Transactional
    public NotificationPreference findOrCreate(String userId) {
        return prefRepo.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Création des préférences par défaut pour userId={}", userId);
                    NotificationPreference newPref = NotificationPreference.builder()
                            .userId(userId)
                            .build();
                    return prefRepo.save(newPref);
                });
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private PreferenceResponse toResponse(NotificationPreference pref) {
        return PreferenceResponse.builder()
                .id(pref.getId())
                .userId(pref.getUserId())
                .emailEnabled(pref.isEmailEnabled())
                .smsEnabled(pref.isSmsEnabled())
                .admissionNotifications(pref.isAdmissionNotifications())
                .paymentNotifications(pref.isPaymentNotifications())
                .courseNotifications(pref.isCourseNotifications())
                .gradeNotifications(pref.isGradeNotifications())
                .attendanceNotifications(pref.isAttendanceNotifications())
                .updatedAt(pref.getUpdatedAt())
                .build();
    }
}
