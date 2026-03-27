package org.darius.course.services;

import org.darius.course.dtos.requests.CancelSessionRequest;
import org.darius.course.dtos.requests.MarkAttendanceRequest;
import org.darius.course.dtos.requests.RescheduleSessionRequest;
import org.darius.course.dtos.responses.AttendanceResponse;
import org.darius.course.dtos.responses.SessionResponse;
import java.util.List;

public interface SessionService {

    SessionResponse getById(Long id);

    List<SessionResponse> getByMatiereAndSemester(Long matiereId, Long semesterId);

    /**
     * Marque les présences d'une séance.
     * Passe la session en DONE.
     * Déclenche la vérification du seuil d'absences.
     */
    List<AttendanceResponse> markAttendance(Long sessionId, String teacherId, MarkAttendanceRequest request);

    /**
     * Annule une séance.
     * Publie SessionCancelledEvent.
     */
    SessionResponse cancel(Long sessionId, CancelSessionRequest request);

    /**
     * Reporte une séance.
     * Crée une nouvelle Session.
     */
    SessionResponse reschedule(Long sessionId, RescheduleSessionRequest request);

    /**
     * Génère les sessions récurrentes pour la semaine à venir.
     * Appelé par le scheduler chaque lundi.
     */
    void generateWeeklySessions();
}
