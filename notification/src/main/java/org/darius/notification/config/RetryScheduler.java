package org.darius.notification.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.notification.services.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetryScheduler {

    private final NotificationService notificationService;

    // Toutes les 5 minutes
    @Scheduled(fixedDelay = 300_000)
    public void retryFailedNotifications() {
        log.debug("Retry scheduler déclenché");
        notificationService.retryFailed();
    }
}
