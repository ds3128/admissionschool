package org.darius.authservice.services;

import org.darius.authservice.common.dtos.*;
import org.darius.authservice.entities.Users;
import org.darius.authservice.exceptions.InvalidTokenException;
import org.darius.authservice.exceptions.PasswordMismatchException;
import org.darius.authservice.exceptions.UserAlreadyExistException;
import org.darius.authservice.exceptions.UserNotFoundException;

public interface UserService {
    UserDtoResponse registration(CreateUserDtoRequest createUserDtoRequest) throws UserAlreadyExistException, PasswordMismatchException;
    // Activation de compte
    void activateAccount(ActivationAccountRequest activationRequest) throws InvalidTokenException, UserNotFoundException;

    // Recherche d'utilisateur
    Users findByUsername(String username);
    UserDtoResponse findByEmail(String email);
    UserDtoResponse findById(Long userId);

    // Mot de passe oublié
    void forgotPassword(ForgotPasswordRequest forgotPasswordRequest);

    // Réinitialisation avec token
    void resetPassword(ResetPasswordRequest resetPasswordRequest);

    // Changement de mot de passe (utilisateur connecté)
    void changePassword(ChangePasswordRequest changePasswordRequest);

    // Connexion - Retourne les tokens !
    AuthResponse login(LoginRequest loginRequest);

    // Déconnexion - Invalide le token
    void logout(LogoutRequest logoutRequest);

    // Rafraîchissement du token
    AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    // Validation de token (pour Gateway)
    TokenValidationResponse validateToken(String token);
}
