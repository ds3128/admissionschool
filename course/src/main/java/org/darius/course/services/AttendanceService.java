package org.darius.course.services;

import org.darius.course.dtos.requests.JustifyAbsenceRequest;
import org.darius.course.dtos.responses.AttendanceResponse;
import org.darius.course.dtos.responses.AttendanceStatsResponse;
import java.util.List;

public interface AttendanceService {

    AttendanceResponse getById(Long id);

    List<AttendanceResponse> getBySession(Long sessionId);

    List<AttendanceStatsResponse> getStatsByStudentAndSemester(String studentId, Long semesterId);

    AttendanceStatsResponse getStatsByStudentAndMatiereAndSemester(
            String studentId, Long matiereId, Long semesterId
    );

    /** Justifie une absence après coup. */
    AttendanceResponse justify(Long attendanceId, JustifyAbsenceRequest request);

    /**
     * Vérifie le seuil d'absences pour un étudiant et une matière.
     * Si dépassé : Enrollment → BLOCKED + publie AttendanceThresholdExceeded.
     */
    void checkThreshold(String studentId, Long matiereId, Long semesterId);

    /**
     * Scheduler quotidien — vérifie tous les seuils d'absences.
     */
    void checkAllThresholds();
}