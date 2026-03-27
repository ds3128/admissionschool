package org.darius.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;


@Service
@RequiredArgsConstructor
public class JwtValidationService {

    @Value("${jwt.secret}")
    private String secretKey;

    private final RedisTemplate<String, String> redisTemplate;

    public boolean isValidToken(String token) {
        try {
            Claims claims = parse(token);

            String jti = claims.getId();
            Boolean blacklisted = redisTemplate.hasKey("blacklist:" + jti);

            return !Boolean.TRUE.equals(blacklisted);
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    public String extractUserId(String token) {
        return (String) parse(token).get("userId");
    }

    public String extractRole(String token) {
        return (String) parse(token).get("role");
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}
