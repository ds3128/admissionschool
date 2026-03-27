package org.darius.course.services;

import org.darius.course.dtos.requests.CreateSemesterRequest;
import org.darius.course.dtos.responses.SemesterResponse;
import java.util.List;

public interface SemesterService {

    SemesterResponse create(CreateSemesterRequest request);

    SemesterResponse getById(Long id);

    SemesterResponse getCurrent();

    List<SemesterResponse> getAll();

    /** Clôture un semestre ACTIVE → CLOSED. */
    SemesterResponse close(Long id);

    /**
     * Calcule la progression semestrielle de tous les étudiants.
     * Semestre doit être CLOSED.
     */
    void computeProgress(Long semesterId);

    /**
     * Valide le semestre CLOSED → VALIDATED.
     * Publie SemesterValidated sur Kafka.
     */
    SemesterResponse validate(Long semesterId);

    /** Scheduler : UPCOMING → ACTIVE si startDate atteinte. */
    void updateSemesterStatuses();
}
