package org.darius.authservice.services;

import org.darius.authservice.common.dtos.*;
import org.darius.authservice.entities.Users;
import org.darius.authservice.events.ApplicationAcceptedEvent;
import org.darius.authservice.exceptions.*;

public interface UserService {
    UserDtoResponse registration(CreateUserDtoRequest createUserDtoRequest) throws UserAlreadyExistException, PasswordMismatchException;
    void activateAccount(String token) throws InvalidTokenException, UserNotFoundException;
    Users findByUsername(String username) throws UserNotFoundException;
    UserDtoResponse findByEmail(String email) throws UserNotFoundException;
    UserDtoResponse findById(String userId) throws UserNotFoundException;
    void forgotPassword(ForgotPasswordRequest forgotPasswordRequest) throws UserNotFoundException;
    void resetPassword(ResetPasswordRequest resetPasswordRequest) throws InvalidTokenException, UserNotFoundException, PasswordMismatchException;
    void changePassword(ChangePasswordRequest changePasswordRequest) throws PasswordMismatchException;
    AuthResponse login(LoginRequest loginRequest) throws UserNotFoundException, TooManyAttemptsException;
    void logout(String token);
    AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) throws UserNotFoundException;
    void resendActivationEmail(String email) throws UserNotFoundException;
    void revokeAllSessions(String email) throws UserNotFoundException;
    void createInstitutionalAccount(ApplicationAcceptedEvent event);
}
