package org.darius.userservice.services.impl;

import org.darius.userservice.common.dtos.CreateMinimalProfilRequest;
import org.darius.userservice.common.dtos.requests.UpdateProfileRequest;
import org.darius.userservice.common.dtos.responses.UserProfileResponse;
import org.darius.userservice.mappers.UserMapper;
import org.darius.userservice.services.UserProfileService;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;
import org.darius.userservice.entities.UserProfile;
import org.darius.userservice.common.enums.Gender;
import org.darius.userservice.exceptions.InvalidOperationException;
import org.darius.userservice.exceptions.UserNotFoundException;
import org.darius.userservice.repositories.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024L; // 2 Mo
    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/png");

    private final UserProfileRepository profileRepository;
    private final UserMapper userMapper;

    public UserProfileServiceImpl(UserProfileRepository profileRepository, UserMapper userMapper) {
        this.profileRepository = profileRepository;
        this.userMapper = userMapper;
    }

    // ── Création depuis Kafka ─────────────────────────────────────────────────

    @Override
    @Transactional
    public void createMinimalProfile(CreateMinimalProfilRequest request) {
        if (profileRepository.existsByUserId(request.getUserId())) {
            log.info("Profil déjà existant pour userId={} - ignoré (idempotence)", request.getUserId());
            return;
        }

        UserProfile profile = UserProfile.builder()
                .userId(request.getUserId())
                .personalEmail(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        profileRepository.save(profile);
        log.info("Profil minimal créé pour userId={}", request.getUserId());
    }

    @Override
    @Transactional
    public void createFullProfileFromAdmission(
            String userId,
            String firstName, String lastName,
            String phone, String nationality,
            String gender, String birthPlace,
            LocalDate birthDate,
            String photoUrl, String personalEmail
    ) {
        if (profileRepository.existsByUserId(userId)) {
            log.info("Profil déjà existant pour userId={} — ignoré (idempotence)", userId);
            return;
        }

        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .nationality(nationality)
                .gender(parseGender(gender))
                .birthPlace(birthPlace)
                .birthDate(birthDate)
                .avatarUrl(photoUrl)
                .personalEmail(personalEmail)
                .build();

        profileRepository.save(profile);
        log.info("Profil complet créé depuis admission pour userId={}", userId);
    }

    // ── Consultation ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String userEmail) {
        return userMapper.toUserProfileResponse(findByEmailOrThrow(userEmail));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByUserId(String userId) {
        return userMapper.toUserProfileResponse(
                profileRepository.findByUserId(userId)
                        .orElseThrow(() -> new UserNotFoundException(
                                "Profil introuvable pour userId=" + userId
                        ))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile findById(String userId){
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found !"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByEmail(String email) {
        return userMapper.toUserProfileResponse(
                profileRepository.findByPersonalEmail(email)
                        .orElseThrow(() -> new UserNotFoundException(
                                "Profil introuvable pour email=" + email
                        ))
        );
    }

    // ── Modification ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(String userEmail, UpdateProfileRequest request) {
        UserProfile profile = findByEmailOrThrow(userEmail);

        // Applique uniquement les champs non-null (partial update via MapStruct)
        userMapper.updateProfileFromRequest(profile, request);

        UserProfile userProfile = profileRepository.save(profile);

        return userMapper.toUserProfileResponse(userProfile);
    }

    @Override
    @Transactional
    public UserProfileResponse uploadAvatar(String userEmail, MultipartFile file) {
        validateAvatarFile(file);

        UserProfile profile = findByEmailOrThrow(userEmail);

        // TODO : stocker sur S3/MinIO et retourner l'URL publique
        // Pour l'instant on stocke le nom du fichier comme placeholder
        String fileUrl = "/avatars/" + profile.getUserId() + "_" + file.getOriginalFilename();
        profile.setAvatarUrl(fileUrl);

        log.info("Avatar mis à jour pour userId={}", profile.getUserId());
        return userMapper.toUserProfileResponse(profileRepository.save(profile));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UserProfile findByEmailOrThrow(String email) {
        return profileRepository.findByPersonalEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "Profil introuvable pour email=" + email
                ));
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException("Le fichier est vide");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new InvalidOperationException(
                    "Fichier trop volumineux. Taille maximale : 2 Mo"
            );
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new InvalidOperationException(
                    "Format non supporté. Formats acceptés : JPEG, PNG"
            );
        }
    }

    private Gender parseGender(String gender) {
        if (gender == null) return null;
        try {
            return Gender.valueOf(gender.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Gender.MALE;
        }
    }

    @Override
    @Transactional
    public void blockStudent(String userId, String reason) {
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setBlocked(true);
            profile.setBlockReason(reason);
            profile.setBlockedAt(LocalDateTime.now());
            profileRepository.save(profile);
            log.info("Étudiant bloqué : userId={}, raison={}", userId, reason);
        });
    }

    @Override
    @Transactional
    public void unblockStudent(String userId) {
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setBlocked(false);
            profile.setBlockReason(null);
            profile.setBlockedAt(null);
            profileRepository.save(profile);
            log.info("Étudiant débloqué : userId={}", userId);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findByUserId(String userId) {

        return this.profileRepository.findByUserId(userId);
    }

}