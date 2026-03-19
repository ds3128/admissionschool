package org.darius.userservice.services;

import org.darius.userservice.common.dtos.requests.CreateTeacherRequest;
import org.darius.userservice.common.dtos.requests.UpdateTeacherRequest;
import org.darius.userservice.common.dtos.responses.PageResponse;
import org.darius.userservice.common.dtos.responses.TeacherResponse;
import org.darius.userservice.common.dtos.responses.TeacherSummaryResponse;

public interface TeacherService {

    /**
     * Crée un profil enseignant manuellement.
     * Coordonne avec l'Auth Service pour la création du compte
     * et publie TeacherProfileCreated.
     */
    TeacherResponse createTeacher(CreateTeacherRequest request, String createdByUserId);

    /**
     * Retourne le profil complet d'un enseignant par son ID.
     */
    TeacherResponse getTeacherById(String teacherId);

    /**
     * Retourne le profil de l'enseignant connecté.
     */
    TeacherResponse getMyTeacherProfile(String userEmail);

    /**
     * Liste paginée des enseignants avec filtres optionnels.
     */
    PageResponse<TeacherSummaryResponse> getTeachers(
            Long departmentId,
            Boolean isActive,
            int page,
            int size
    );

    /**
     * Met à jour les informations professionnelles d'un enseignant.
     */
    TeacherResponse updateTeacher(String teacherId, UpdateTeacherRequest request);

    /**
     * Désactive un enseignant.
     * Publie TeacherDeactivated.
     */
    TeacherResponse deactivateTeacher(String teacherId);

    /**
     * Réactive un enseignant précédemment désactivé.
     */
    TeacherResponse reactivateTeacher(String teacherId);
}