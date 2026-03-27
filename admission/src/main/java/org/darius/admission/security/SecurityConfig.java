package org.darius.admission.security;

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

    private final AdmissionSecurityFilter admissionSecurityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Swagger + Actuator
                        .requestMatchers(
                                "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**"
                        ).permitAll()

                        // Routes publiques — lecture sans JWT
                        .requestMatchers(HttpMethod.GET, "/admissions/campaigns/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admissions/offers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admissions/required-documents/**").permitAll()

                        // Candidat — candidatures
                        .requestMatchers("/admissions/applications/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN_SCHOLAR", "CANDIDATE", "STUDENT")

                        // Administration
                        .requestMatchers("/admissions/admin/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN_SCHOLAR")

                        // Commissions — lecture et vote
                        .requestMatchers(HttpMethod.GET, "/admissions/commissions/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN_SCHOLAR", "TEACHER")
                        .requestMatchers(HttpMethod.POST, "/admissions/commissions/**")
                        .hasAnyRole("SUPER_ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/admissions/commissions/**")
                        .hasRole("SUPER_ADMIN")

                        // Entretiens
                        .requestMatchers("/admissions/interviews/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN_SCHOLAR", "TEACHER")

                        // Directeurs de thèse
                        .requestMatchers("/admissions/thesis-approvals/**")
                        .hasAnyRole("SUPER_ADMIN", "TEACHER")

                        // Campagnes — modifications admin uniquement
                        .requestMatchers(HttpMethod.POST, "/admissions/campaigns/**")
                        .hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/admissions/campaigns/**")
                        .hasRole("SUPER_ADMIN")

                        // Offres — modifications admin uniquement
                        .requestMatchers(HttpMethod.POST, "/admissions/offers/**")
                        .hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/admissions/offers/**")
                        .hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admissions/required-documents/**")
                        .hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/admissions/required-documents/**")
                        .hasRole("SUPER_ADMIN")

                        // Liste d'attente
                        .requestMatchers("/admissions/waitlist/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN_SCHOLAR")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(admissionSecurityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}