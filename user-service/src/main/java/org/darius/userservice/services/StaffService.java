package org.darius.userservice.services;

import org.darius.userservice.common.dtos.requests.CreateStaffRequest;
import org.darius.userservice.common.dtos.responses.PageResponse;
import org.darius.userservice.common.dtos.responses.StaffResponse;

public interface StaffService {

    /**
     * Crée un profil de personnel administratif.
     * Coordonne avec l'Auth Service pour la création du compte
     * et publie StaffProfileCreated.
     */
    StaffResponse createStaff(CreateStaffRequest request, String createdByUserId);

    /**
     * Retourne le profil d'un membre du personnel par son ID.
     */
    StaffResponse getStaffById(String staffId);

    /**
     * Retourne le profil du personnel connecté.
     */
    StaffResponse getMyStaffProfile(String userEmail);

    /**
     * Liste paginée du personnel avec filtre optionnel par département.
     */
    PageResponse<StaffResponse> getStaffMembers(
            Long departmentId,
            Boolean isActive,
            int page,
            int size
    );

    /**
     * Désactive un membre du personnel.
     */
    StaffResponse deactivateStaff(String staffId);
}