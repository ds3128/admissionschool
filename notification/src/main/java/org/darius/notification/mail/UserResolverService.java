package org.darius.notification.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserResolverService {

    private final RestClient restClient;

    @Value("${user-service.base-url}")
    private String userServiceBaseUrl;

    // Cache en mémoire pour éviter les appels répétitifs
    private final Map<String, UserInfo> cache = new ConcurrentHashMap<>();

    // ── Résolution complète ────────────────────────────────────────────────────

    public UserInfo resolve(String userId) {
        if (userId == null || userId.isBlank()) return null;

        if (cache.containsKey(userId)) {
            return cache.get(userId);
        }

        try {
            Map response = restClient.get()
                    .uri(userServiceBaseUrl + "/users/" + userId)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return null;

            String email = (String) response.getOrDefault("email",
                    response.getOrDefault("personalEmail", null));
            String firstName = (String) response.getOrDefault("firstName", "");
            String lastName  = (String) response.getOrDefault("lastName",  "");

            UserInfo info = new UserInfo(email, firstName, lastName);
            if (email != null) cache.put(userId, info);
            return info;

        } catch (Exception ex) {
            log.warn("Résolution userId={} échouée : {}", userId, ex.getMessage());
            return null;
        }
    }

    // ── Helpers raccourcis ────────────────────────────────────────────────────

    public String resolveEmail(String userId) {
        UserInfo info = resolve(userId);
        return info != null ? info.email() : null;
    }

    public String resolveFirstName(String userId) {
        UserInfo info = resolve(userId);
        return info != null ? info.firstName() : null;
    }

    // ── Record interne ────────────────────────────────────────────────────────

    public record UserInfo(String email, String firstName, String lastName) {}
}
