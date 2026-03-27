package org.darius.course.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityFilter courseSecurityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                        // Lecture publique (authentifié)
                        .requestMatchers(HttpMethod.GET,
                                "/courses/semesters/**",
                                "/courses/teaching-units/**",
                                "/courses/matieres/**",
                                "/courses/rooms/**",
                                "/courses/groups/**",
                                "/courses/slots/**",
                                "/courses/schedule",
                                "/courses/sessions/**",
                                "/courses/enrollments/**",
                                "/courses/evaluations/**"
                        ).authenticated()
                        // Enseignant
                        .requestMatchers(
                                "/courses/sessions/*/attendance",
                                "/courses/evaluations",
                                "/courses/evaluations/*/grades",
                                "/courses/evaluations/*/publish",
                                "/courses/matieres/*/resources",
                                "/courses/resources/**",
                                "/courses/evaluations/*/attachments"
                        ).hasAnyRole("TEACHER", "ADMIN_SCHOLAR", "SUPER_ADMIN")
                        // Administration
                        .requestMatchers(
                                "/courses/semesters/*/close",
                                "/courses/semesters/*/compute-progress",
                                "/courses/slots",
                                "/courses/assignments/**",
                                "/courses/groups/*/students",
                                "/courses/enrollments/*/status"
                        ).hasAnyRole("ADMIN_SCHOLAR", "SUPER_ADMIN")
                        // Super admin uniquement
                        .requestMatchers("/courses/semesters/*/validate")
                        .hasRole("SUPER_ADMIN")
                        // ML
                        .requestMatchers("/courses/ml/**")
                        .hasAnyRole("ADMIN_SCHOLAR", "SUPER_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(courseSecurityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
