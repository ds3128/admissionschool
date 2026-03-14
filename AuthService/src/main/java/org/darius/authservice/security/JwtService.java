package org.darius.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.authservice.entities.Jwt;
import org.darius.authservice.entities.RefreshToken;
import org.darius.authservice.entities.Users;
import org.darius.authservice.exceptions.UserNotFoundException;
import org.darius.authservice.repositories.JwtRepository;
import org.darius.authservice.services.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class JwtService {
    private static final String ENCRYPTION_KEY = "g6QXiyaSf2GYDpfAQydAa1TB8p00rXD2OWTiNQfieiVcOGfO2P7zLXwm37FYekBKDQtyICARczOlzXmKZO9roQ";
    private static final long VERIFICATION_TOKEN_EXPIRATION = 24 * 60 * 60 * 1000L; // 24h
    private final String INVALID_TOKEN = "Invalid token";
    private final String ACCESS_TOKEN = "Access Token";
    private final String REFRESH_TOKEN = "Refresh Token";
    private static final long ACCESS_TOKEN_EXPIRATION = 30 * 60 * 1000; // thirty minutes
    private final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000; // seven days

    private final JwtRepository  jwtRepository;

    private final UserService  userService;

    public Jwt tokenByValue(String tokenValue) {
        return this.jwtRepository.findJwtByValueAndDeactivateAndExpire(
                tokenValue,
                false,
                false
        ).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Map<String, String> generate(String username) throws UserNotFoundException {
        Users user = this.userService.findByUsername(username);
        this.disabledToken(user);

        Map<String,String> tokens = generateJwt(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .value(tokens.get(REFRESH_TOKEN))
                .expire(false)
                .createdAt(Instant.now())
                .expiredAt(Instant.ofEpochMilli(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .build();

        Jwt jwt = Jwt.builder()
                .value(tokens.get(ACCESS_TOKEN))
                .deactivate(false)
                .expire(false)
                .user(user)
                .refreshToken(refreshToken)
                .build();

        this.jwtRepository.save(jwt);
        return tokens;
    }

    private Map<String, String> generateJwt(Users user) {
        return Map.of(
                ACCESS_TOKEN, createJwtToken(user.getEmail(), Map.of("name", user.getUsername()), ACCESS_TOKEN_EXPIRATION),
                REFRESH_TOKEN, createJwtToken(user.getEmail(), Map.of(
                        "typ", "refresh",
                        "jti", UUID.randomUUID().toString()
                ), REFRESH_TOKEN_EXPIRATION)
        );
    }

    private String createJwtToken(String subject, Map<String,Object> claims, long expirationTime) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .subject(subject)
                .claims(claims)
                .signWith(getKey())
                .compact();
    }

    private Key getKey() {
        byte[] decodedKey = Decoders.BASE64.decode(ENCRYPTION_KEY);
        return Keys.hmacShaKeyFor(decodedKey);
    }

    private void disabledToken(Users user) {
        List<Jwt> jwtList = this.jwtRepository.findUserByEmail(user.getEmail())
                .peek(jwt -> {
                    jwt.setDeactivate(true);
                    jwt.setExpire(true);
                })
                .collect(Collectors.toList());
        this.jwtRepository.saveAll(jwtList);
    }

    public String extractUsername(String token) {
        return getClaim(token, Claims::getSubject);
    }

    private <T> T getClaim(String token, Function<Claims, T> function) {
        Claims claims = getAllClaims(token);
        return function.apply(claims);
    }

    private Claims getAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public void disconnect() {
        Users user = (Users) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        assert user != null;
        Jwt jwt = this.jwtRepository.findUserValidToken(
                user.getEmail(),
                false,
                false
        ).orElseThrow(() -> new RuntimeException(INVALID_TOKEN));
        jwt.setExpire(true);
        jwt.setDeactivate(true);

        this.jwtRepository.save(jwt);
    }

    public Map<String, String> refreshToken(Map<String, String> refreshToken) throws UserNotFoundException {
        Jwt jwt = this.jwtRepository.findUserByRefreshToken(refreshToken.get(REFRESH_TOKEN))
                .orElseThrow(() -> new RuntimeException("Invalid Token"));
        if (jwt.getRefreshToken().isExpire() || jwt.getRefreshToken().getExpiredAt().isBefore(Instant.now())) {
            throw new RuntimeException("Invalid Token");
        }
        this.disabledToken(jwt.getUser());
        return this.generate(jwt.getUser().getEmail());
    }

    @Scheduled(cron = "@daily")
    public void removeUselessJwt() {
        log.info("Deleting token at {}", Instant.now());
        this.jwtRepository.deleteAllByExpireAndDeactivate(true, true);
    }

    public boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        return expirationDate.before(new Date());
    }

    // Génère un token JWT dédié à la vérification du compte (24h)
    public String generateVerificationToken(String email) {
        return createJwtToken(email, new HashMap<>(), VERIFICATION_TOKEN_EXPIRATION);
    }

    // Extrait l'email depuis le token de vérification
    public String extractEmailFromVerificationToken(String token) {
        return getClaim(token, Claims::getSubject);
    }

    // Vérifie que le token de vérification est valide et non expiré
    public boolean isVerificationTokenValid(String token) {
        try {
            return !getClaim(token, Claims::getExpiration).before(new Date());
        } catch (JwtException e) {
            return false;
        }
    }

    private Date getExpirationDateFromToken(String token) {
        return getClaim(token, Claims::getExpiration);
    }
}
