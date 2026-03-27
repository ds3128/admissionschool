package org.darius.notification.securities;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final NotificationSecurityFilter notificationSecurityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**"
                        ).permitAll()
                        // Utilisateur authentifié — ses propres notifications
                        .requestMatchers(HttpMethod.GET,
                                "/notifications/me",
                                "/notifications/me/unread",
                                "/notifications/me/count"
                        ).authenticated()
                        .requestMatchers(HttpMethod.PUT,
                                "/notifications/*/read",
                                "/notifications/me/read-all"
                        ).authenticated()
                        // Préférences
                        .requestMatchers("/notifications/preferences").authenticated()
                        // Administration
                        .requestMatchers("/notifications/admin/**")
                        .hasAnyRole("ADMIN_SCHOLAR", "SUPER_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        notificationSecurityFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }
}
