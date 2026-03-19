package org.darius.userservice.services;

import org.darius.userservice.common.dtos.requests.CreateFiliereRequest;
import org.darius.userservice.common.dtos.requests.UpdateFiliereStatusRequest;
import org.darius.userservice.common.dtos.responses.FiliereResponse;
import org.darius.userservice.common.dtos.responses.StudyLevelResponse;
import org.darius.userservice.common.enums.FiliereStatus;

import java.util.List;

public interface FiliereService {

    /**
     * Crée une filière avec ses StudyLevel générés automatiquement.
     * Le nombre de niveaux = durationYears.
     */
    FiliereResponse createFiliere(CreateFiliereRequest request);

    /**
     * Retourne toutes les filières, avec filtre optionnel sur le statut.
     */
    List<FiliereResponse> getAllFilieres(FiliereStatus status);

    /**
     * Retourne une filière par son ID.
     */
    FiliereResponse getFiliereById(Long filiereId);

    /**
     * Retourne les niveaux d'étude d'une filière.
     */
    List<StudyLevelResponse> getStudyLevelsByFiliere(Long filiereId);

    /**
     * Retourne un StudyLevel par son ID.
     */
    StudyLevelResponse getStudyLevelById(Long levelId);

    /**
     * Retourne le premier StudyLevel d'une filière (order = 1).
     */
    StudyLevelResponse getFirstLevelByFiliere(Long filiereId);

    /**
     * Retourne le StudyLevel suivant dans la filière (order + 1).
     * Retourne null si l'étudiant est au dernier niveau.
     */
    StudyLevelResponse getNextLevel(Long currentLevelId);

    /**
     * Vérifie si un StudyLevel est le dernier de sa filière.
     */
    boolean isLastLevel(Long levelId);

    /**
     * Change le statut d'une filière (ACTIVE → INACTIVE → ARCHIVED).
     */
    FiliereResponse updateFiliereStatus(Long filiereId, UpdateFiliereStatusRequest request);
}