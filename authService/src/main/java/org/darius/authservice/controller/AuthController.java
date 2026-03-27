package org.darius.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.darius.authservice.common.dtos.*;
import org.darius.authservice.exceptions.InvalidTokenException;
import org.darius.authservice.exceptions.PasswordMismatchException;
import org.darius.authservice.exceptions.UserAlreadyExistException;
import org.darius.authservice.exceptions.UserNotFoundException;
import org.darius.authservice.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Gestion de l'authentification et des comptes")
public class AuthController {

    private final UserService userService;

    @Operation(summary = "Créer un compte utilisateur",
            description = "Crée un nouveau compte avec isActive=false et envoie un mail d'activation")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte créé avec succès"),
            @ApiResponse(responseCode = "409", description = "Email déjà utilisé"),
            @ApiResponse(responseCode = "400", description = "Mots de passe non conformes")
    })
    @PostMapping("/register")
    public ResponseEntity<UserDtoResponse> register(
            @RequestBody CreateUserDtoRequest request
    ) throws UserAlreadyExistException, PasswordMismatchException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.registration(request));
    }

    @Operation(summary = "Activer son compte",
            description = "Valide le token JWT reçu par mail et active le compte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte activé avec succès"),
            @ApiResponse(responseCode = "401", description = "Token invalide ou expiré"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @GetMapping("/verify")
    public ResponseEntity<String> verifyAccount(
            @Parameter(description = "Token JWT reçu par mail", required = true)
            @RequestParam String token
    ) throws InvalidTokenException, UserNotFoundException {
        userService.activateAccount(token);
        return ResponseEntity.ok("Account activated successfully.");
    }

    @Operation(summary = "Renvoyer le mail d'activation",
            description = "Génère un nouveau token et renvoie le mail si le précédent a expiré")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mail renvoyé avec succès"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @PostMapping("/resend-activation")
    public ResponseEntity<String> resendActivation(
            @Parameter(description = "Email du compte à activer", required = true)
            @RequestParam String email
    ) throws UserNotFoundException {
        userService.resendActivationEmail(email);
        return ResponseEntity.ok("Activation email resent successfully.");
    }

    @Operation(summary = "Se connecter",
            description = "Retourne un access token (30min) et un refresh token (7j)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie"),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides"),
            @ApiResponse(responseCode = "429", description = "Trop de tentatives - compte temporairement bloqué")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(userService.login(request));
    }

    @Operation(summary = "Se déconnecter",
            description = "Invalide le token courant en base et dans la blacklist Redis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Déconnexion réussie"),
            @ApiResponse(responseCode = "401", description = "Token invalide")
    })
    @SecurityRequirement(name = "Bearer Auth")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestParam String token
    ) {
        userService.logout(token);
        return ResponseEntity.ok("Logged out successfully.");
    }

    @Operation(summary = "Rafraîchir le token",
            description = "Génère une nouvelle paire access/refresh token à partir du refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens rafraîchis"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalide ou expiré")
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(userService.refreshToken(request));
    }

    @Operation(summary = "Révoquer toutes les sessions",
            description = "Déconnecte l'utilisateur de tous ses appareils - réservé ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessions révoquées"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @SecurityRequirement(name = "Bearer Auth")
    @PostMapping("/revoke-all-sessions")
    public ResponseEntity<String> revokeAllSessions(
            @Parameter(description = "Email de l'utilisateur", required = true)
            @RequestParam String email
    ) throws UserNotFoundException {
        userService.revokeAllSessions(email);
        return ResponseEntity.ok("All sessions revoked successfully.");
    }

    @Operation(summary = "Changer son mot de passe",
            description = "Requiert l'ancien mot de passe - révoque le token courant après changement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe modifié"),
            @ApiResponse(responseCode = "400", description = "Ancien mot de passe incorrect ou confirmation invalide")
    })
    @SecurityRequirement(name = "Bearer Auth")
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody ChangePasswordRequest request
    ) throws PasswordMismatchException {
        userService.changePassword(request);
        return ResponseEntity.ok("Password changed successfully.");
    }

    @Operation(summary = "Mot de passe oublié",
            description = "Envoie un lien de réinitialisation par mail (JWT 24h). Réponse neutre pour éviter l'énumération d'emails")
    @ApiResponse(responseCode = "200", description = "Mail envoyé si l'email existe")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @RequestBody ForgotPasswordRequest request
    ) {
        userService.forgotPassword(request);
        return ResponseEntity.ok("If this email exists, a reset link has been sent.");
    }

    @Operation(summary = "Réinitialiser le mot de passe",
            description = "Valide le token de reset et enregistre le nouveau mot de passe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe réinitialisé"),
            @ApiResponse(responseCode = "401", description = "Token invalide ou expiré"),
            @ApiResponse(responseCode = "400", description = "Confirmation non conforme")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestBody ResetPasswordRequest request
    ) throws InvalidTokenException, PasswordMismatchException {
        userService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully.");
    }

    @Operation(summary = "Rechercher un utilisateur par ID")
    @SecurityRequirement(name = "Bearer Auth")
    @GetMapping("/user/{id}")
    public ResponseEntity<UserDtoResponse> findById(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Operation(summary = "Rechercher un utilisateur par email")
    @SecurityRequirement(name = "Bearer Auth")
    @GetMapping("/user")
    public ResponseEntity<UserDtoResponse> findByEmail(
            @Parameter(description = "Email de l'utilisateur", required = true)
            @RequestParam String email
    ) {
        return ResponseEntity.ok(userService.findByEmail(email));
    }
}