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
    Users findByUsername(String username) throws UserNotFoundException;
    UserDtoResponse findByEmail(String email) throws UserNotFoundException;
    UserDtoResponse findById(String userId) throws UserNotFoundException;

    // Mot de passe oublié
    void forgotPassword(ForgotPasswordRequest forgotPasswordRequest) throws UserNotFoundException;

    // Réinitialisation avec token
    void resetPassword(ResetPasswordRequest resetPasswordRequest) throws InvalidTokenException, UserNotFoundException, PasswordMismatchException;

    // Changement de mot de passe (utilisateur connecté)
    void changePassword(ChangePasswordRequest changePasswordRequest) throws PasswordMismatchException;

    // Connexion - Retourne les tokens !
    AuthResponse login(LoginRequest loginRequest) throws UserNotFoundException;

    // Déconnexion - Invalide le token
    void logout(String token);

    // Rafraîchissement du token
    AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) throws UserNotFoundException;

    // Validation de token (pour Gateway)
    TokenValidationResponse validateToken(String token);

    void resendActivationEmail(String email) throws UserNotFoundException;
}
