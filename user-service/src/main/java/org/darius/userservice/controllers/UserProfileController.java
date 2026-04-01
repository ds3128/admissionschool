package org.darius.userservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.darius.userservice.common.dtos.requests.UpdateProfileRequest;
import org.darius.userservice.common.dtos.responses.UserProfileResponse;
import org.darius.userservice.entities.UserProfile;
import org.darius.userservice.services.UserProfileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Profil utilisateur", description = "Gestion du profil personnel")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/internal/{userId}")
    public ResponseEntity<Map<String, String>> getInternalUser(@PathVariable String userId) {
        return userProfileService.findByUserId(userId)
                .map(user -> {
                    Map<String, String> result = new HashMap<>();
                    result.put("email",     user.getPersonalEmail() != null ? user.getPersonalEmail() : "");
                    result.put("firstName", user.getFirstName()     != null ? user.getFirstName()     : "");
                    result.put("lastName",  user.getLastName()      != null ? user.getLastName()      : "");
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/me")
    @Operation(summary = "Mon profil")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @RequestHeader("X-User-Email") String userEmail
    ) {
        return ResponseEntity.ok(userProfileService.getMyProfile(userEmail));
    }

    @PutMapping("/me")
    @Operation(summary = "Mettre à jour mon profil")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @RequestHeader("X-User-Email") String userEmail,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userProfileService.updateMyProfile(userEmail, request));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader ma photo de profil")
    public ResponseEntity<UserProfileResponse> uploadAvatar(
            @RequestHeader("X-User-Email") String userEmail,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(userProfileService.uploadAvatar(userEmail, file));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Profil par userId")
    public ResponseEntity<UserProfileResponse> getProfileByUserId(
            @PathVariable String userId
    ) {
        return ResponseEntity.ok(userProfileService.getProfileByUserId(userId));
    }

    @GetMapping
    @Operation(summary = "Profil par email")
    public ResponseEntity<UserProfileResponse> getProfileByEmail(
            @RequestParam String email
    ) {
        return ResponseEntity.ok(userProfileService.getProfileByEmail(email));
    }
}