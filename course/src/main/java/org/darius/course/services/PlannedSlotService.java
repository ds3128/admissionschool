package org.darius.course.services;

import org.darius.course.dtos.requests.CreatePlannedSlotRequest;
import org.darius.course.dtos.responses.ConflictResponse;
import org.darius.course.dtos.responses.PlannedSlotResponse;
import org.darius.course.dtos.responses.WeeklyScheduleResponse;
import java.time.LocalDate;
import java.util.List;

public interface PlannedSlotService {

    /**
     * Crée un créneau et génère les Sessions associées.
     * Lève une exception si un conflit est détecté.
     */
    PlannedSlotResponse create(CreatePlannedSlotRequest request);

    PlannedSlotResponse update(Long id, CreatePlannedSlotRequest request);

    void delete(Long id);

    PlannedSlotResponse getById(Long id);

    List<PlannedSlotResponse> getBySemester(Long semesterId);

    List<PlannedSlotResponse> getByGroupAndSemester(Long groupId, Long semesterId);

    List<PlannedSlotResponse> getByTeacherAndSemester(String teacherId, Long semesterId);

    /**
     * Détecte les conflits (salle, enseignant, groupe) sans créer le slot.
     * Retourne la liste des conflits détectés.
     */
    List<ConflictResponse> detectConflicts(CreatePlannedSlotRequest request, Long excludeSlotId);

    /**
     * Vue emploi du temps hebdomadaire pour un utilisateur.
     * Filtrée selon le rôle : étudiant (son groupe), enseignant (ses matières), admin (tout).
     */
    WeeklyScheduleResponse getWeeklySchedule(String userId, String role, LocalDate weekDate);
}
