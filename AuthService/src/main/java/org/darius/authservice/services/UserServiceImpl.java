package org.darius.authservice.services;

import org.darius.authservice.common.dtos.*;
import org.darius.authservice.common.enums.RoleType;
import org.darius.authservice.entities.Role;
import org.darius.authservice.entities.Users;
import org.darius.authservice.exceptions.InvalidTokenException;
import org.darius.authservice.exceptions.PasswordMismatchException;
import org.darius.authservice.exceptions.UserAlreadyExistException;
import org.darius.authservice.exceptions.UserNotFoundException;
import org.darius.authservice.mapper.UserMapper;
import org.darius.authservice.repositories.UserRepository;
import org.darius.authservice.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final EmailService emailService;

    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder, JwtService jwtService, UserMapper userMapper, EmailService emailService) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.emailService = emailService;
    }

    @Override
    public UserDtoResponse registration(CreateUserDtoRequest createUserDtoRequest) throws UserAlreadyExistException, PasswordMismatchException {
        Optional<Users> email = this.userRepository.findByEmail(createUserDtoRequest.getEmail());
        if (email.isPresent()) {
            throw new UserAlreadyExistException("Email already exists");
        }

        this.validatePasswordMatch(
                createUserDtoRequest.getPassword(),
                createUserDtoRequest.getConfirmPassword()
        );

        Users user = new Users();
        Role userRole = new Role();

        user.setEmail(createUserDtoRequest.getEmail());
        user.setPassword(this.bCryptPasswordEncoder.encode(createUserDtoRequest.getPassword()));
        user.setLastLogin(null);

        userRole.setRoleType(RoleType.CANDIDATE);
        user.setRole(userRole);

        Users savedUser = this.userRepository.save(user);

        this.sendActivationEmail(savedUser);

        return userMapper.ToUserDtoResponse(savedUser);
    }

    private void sendActivationEmail(Users user) {
        String token = jwtService.generateVerificationToken(user.getEmail());
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Override
    public void activateAccount(ActivationAccountRequest activationRequest) throws InvalidTokenException, UserNotFoundException {
        if (!jwtService.isVerificationTokenValid(activationRequest.getToken())) {
            throw new InvalidTokenException("Token expired");
        }

        String email = jwtService.extractEmailFromVerificationToken(activationRequest.getToken());

        Users user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.isEnabled()){
            throw new InvalidTokenException("This account is already enabled");
        }

        user.setStatus(true);
        userRepository.save(user);
    }

    @Override
    public Users findByUsername(String username) {
        return null;
    }

    @Override
    public UserDtoResponse findByEmail(String email) {
        return null;
    }

    @Override
    public UserDtoResponse findById(Long userId) {
        return null;
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {

    }

    @Override
    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {

    }

    @Override
    public void changePassword(ChangePasswordRequest changePasswordRequest) {

    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        return null;
    }

    @Override
    public void logout(LogoutRequest logoutRequest) {

    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        return null;
    }

    @Override
    public TokenValidationResponse validateToken(String token) {
        return null;
    }

    private void validatePasswordMatch(String password, String confirm) throws PasswordMismatchException {
        if (!Objects.equals(password, confirm)) {
            throw new PasswordMismatchException("Passwords didn't match");
        }
    }
}
