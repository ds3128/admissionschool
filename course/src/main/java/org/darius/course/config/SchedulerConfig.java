package org.darius.course.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.course.services.AttendanceService;
import org.darius.course.services.SemesterService;
import org.darius.course.services.SessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfig {

    private final SemesterService   semesterService;
    private final SessionService    sessionService;
    private final AttendanceService attendanceService;

    // Chaque lundi à 08:00 — vérification sessions semaine à venir
    @Scheduled(cron = "0 0 8 * * MON")
    public void generateWeeklySessions() {
        log.info("Scheduler — génération sessions semaine à venir");
        sessionService.generateWeeklySessions();
    }

    // Chaque jour à 01:00 — vérification seuils d'absences
    @Scheduled(cron = "0 0 1 * * *")
    public void checkAttendanceThresholds() {
        log.info("Scheduler — vérification seuils d'absences");
        attendanceService.checkAllThresholds();
    }

    // Chaque jour à 06:00 — transitions de statut des semestres
    @Scheduled(cron = "0 0 6 * * *")
    public void updateSemesterStatuses() {
        log.info("Scheduler — mise à jour statuts semestres");
        semesterService.updateSemesterStatuses();
    }
}
