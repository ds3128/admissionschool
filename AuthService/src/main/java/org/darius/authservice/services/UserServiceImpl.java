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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
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
        if (jwtService.isVerificationTokenValid(activationRequest.getToken())) {
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
    public Users findByUsername(String username) throws UserNotFoundException {
        return this.userRepository.findByEmail(username).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    public UserDtoResponse findByEmail(String email) throws UserNotFoundException {
        Users user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return userMapper.ToUserDtoResponse(user);
    }

    @Override
    public UserDtoResponse findById(String userId) throws UserNotFoundException {
        Users user = this.userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return userMapper.ToUserDtoResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) throws UserNotFoundException {
        // first : find user by email
        Users user = this.userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        // check if user account is active
        if (!user.isEnabled()) {
            throw new RuntimeException("Account not activated. Please check your email.");
        }
        // check if password match
        if (!bCryptPasswordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // generate user token
        Map<String, String> token = jwtService.generate(user.getEmail());

        // define last user login
        user.setLastLogin(new Date());
        // update user in database
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(token.get("Access Token"))
                .refreshToken(token.get("Refresh Token"))
                .build();
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest forgotPasswordRequest) throws UserNotFoundException {

        Users user = this.userRepository.findByEmail(forgotPasswordRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not activated.");
        }

        String resetToken = jwtService.generateVerificationToken(user.getEmail());

        emailService.sendResetPasswordEmail(user.getEmail(), resetToken);
    }

    @Override
    public void resetPassword(ResetPasswordRequest resetPasswordRequest) throws InvalidTokenException, UserNotFoundException, PasswordMismatchException {
        if (jwtService.isVerificationTokenValid(resetPasswordRequest.getToken())) {
            throw new InvalidTokenException("Token invalid or expired");
        }

        String email = jwtService.extractEmailFromVerificationToken(resetPasswordRequest.getToken());

        Users user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        validatePasswordMatch(resetPasswordRequest.getNewPassword(), resetPasswordRequest.getConfirmPassword());

        user.setPassword(bCryptPasswordEncoder.encode(resetPasswordRequest.getNewPassword()));

        this.userRepository.save(user);
    }

    @Override
    public void changePassword(ChangePasswordRequest changePasswordRequest) throws PasswordMismatchException {
        Authentication authentication = Objects.requireNonNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        Users user = (Users) authentication.getPrincipal();
        String token = (String) authentication.getCredentials();

        assert user != null;
        if (!bCryptPasswordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new PasswordMismatchException("Current password is incorrect.");
        }

        validatePasswordMatch(
                changePasswordRequest.getNewPassword(),
                changePasswordRequest.getConfirmPassword()
        );

        user.setPassword(bCryptPasswordEncoder.encode(changePasswordRequest.getNewPassword()));
        this.userRepository.save(user);

        jwtService.disconnect(token);
    }

    @Override
    public void logout(String token) {
        this.jwtService.disconnect(token);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) throws UserNotFoundException {
        Map<String, String> tokens = this.jwtService.refreshToken(
                Map.of("Refresh Token", refreshTokenRequest.getRefreshToken()));

        return AuthResponse.builder()
                .accessToken(tokens.get("Access Token"))
                .refreshToken(tokens.get("Refresh Token"))
                .build();
    }

    @Override
    public TokenValidationResponse validateToken(String token) {
        return null;
    }

    @Override
    public void resendActivationEmail(String email) throws UserNotFoundException {
        Users user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.isEnabled()) {
            throw new RuntimeException("Account is already activated.");
        }

        this.sendActivationEmail(user);
    }

    private void validatePasswordMatch(String password, String confirm) throws PasswordMismatchException {
        if (!Objects.equals(password, confirm)) {
            throw new PasswordMismatchException("Passwords didn't match");
        }
    }
}
