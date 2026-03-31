package org.darius.authservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    public static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_MINUTES = 15;
    private String key(String email) {
        return "login:attempts:" + email;
    }
    private String blockKey(String email) {
        return "login:blocked:" + email;
    }

    public void registrationFailedAttempt(String email) {
        String key = key(email);

        redisTemplate.opsForValue().increment(key);

        // ttl only at the first attempt
        if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1"))) {
            redisTemplate.expire(key, BLOCK_MINUTES, TimeUnit.MINUTES);
        }

        int attempts = getAttempts(email);

        if (attempts >= MAX_ATTEMPTS) {
            // block user during 15 minutes
            redisTemplate.opsForValue().set(
                    blockKey(email),
                    "true",
                    BLOCK_MINUTES,
                    TimeUnit.MINUTES
            );
            redisTemplate.delete(key);
        }
    }

    public void resetAttempts(String email) {
        redisTemplate.delete(key(email));
        redisTemplate.delete(blockKey(email));
    }

    public boolean isBlocked(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(blockKey(email)));
    }

    public int getAttempts(String email) {
        String val = redisTemplate.opsForValue().get(key(email));
        return val != null ? Integer.parseInt(val) : 0;
    }

    public long getRemainingBlockTime(String email){
        Long ttl = redisTemplate.getExpire(blockKey(email),  TimeUnit.MINUTES);
        return ttl != null ? ttl : 0;
    }
}