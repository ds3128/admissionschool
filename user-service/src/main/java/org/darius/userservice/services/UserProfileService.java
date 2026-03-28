package org.darius.userservice.services;

import org.darius.userservice.common.dtos.requests.UpdateProfileRequest;
import org.darius.userservice.common.dtos.responses.UserProfileResponse;
import org.darius.userservice.entities.UserProfile;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

    /**
     * Crée un profil minimal à la réception de UserActivated (Kafka).
     * Idempotent — ignoré si userId déjà existant.
     */
    void createMinimalProfile(String userId, String email);

    /**
     * Crée un profil complet depuis les données de ApplicationAccepted (Kafka).
     * Idempotent — ignoré si userId déjà existant.
     */
    void createFullProfileFromAdmission(
            String userId,
            String firstName, String lastName,
            String phone, String nationality,
            String gender, String birthPlace,
            java.time.LocalDate birthDate,
            String photoUrl, String personalEmail
    );

    /**
     * Retourne le profil de l'utilisateur connecté.
     * Le userId est extrait du header X-User-Email via la Gateway.
     */
    UserProfileResponse getMyProfile(String userEmail);

    /**
     * Retourne un profil par son userId.
     */
    UserProfileResponse getProfileByUserId(String userId);

    UserProfile findById(String id);

    /**
     * Retourne un profil par email (usage inter-services).
     */
    UserProfileResponse getProfileByEmail(String email);

    /**
     * Met à jour les informations personnelles de l'utilisateur connecté.
     */
    UserProfileResponse updateMyProfile(String userEmail, UpdateProfileRequest request);

    /**
     * Met à jour la photo de profil.
     * Formats acceptés : JPEG, PNG. Taille max : 2 Mo.
     */
    UserProfileResponse uploadAvatar(String userEmail, MultipartFile file);

    void blockStudent(String userId, String reason);
    void unblockStudent(String userId);
}