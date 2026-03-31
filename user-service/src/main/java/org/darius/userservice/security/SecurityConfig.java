package org.darius.userservice.security;

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
@EnableMethodSecurity          // ← active @PreAuthorize dans les controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserSecurityFilter userSecurityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()

                        // Filières et niveaux — lecture publique (Admission Service en a besoin)
                        .requestMatchers(HttpMethod.GET, "/users/filieres/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/departments/**").permitAll()
                        .requestMatchers("/users/internal/**").permitAll()
                        .requestMatchers("/users/students/internal/**").permitAll()

                        // Structures académiques — admin uniquement
                        .requestMatchers(HttpMethod.POST, "/users/departments/**")
                        .hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/users/departments/**")
                        .hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/users/filieres/**")
                        .hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/users/filieres/**")
                        .hasRole("SUPER_ADMIN")

                        // Profil personnel — accessible à tous les utilisateurs connectés
                        .requestMatchers("/users/me/**")
                        .hasAnyRole(
                                "SUPER_ADMIN", "ADMIN_SCHOLAR", "ADMIN_RH",
                                "ADMIN_FINANCE", "TEACHER", "STUDENT"
                        )

                        // Étudiants — lecture pour enseignants et admins
                        .requestMatchers(HttpMethod.GET, "/users/students/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN_SCHOLAR", "TEACHER", "STUDENT")

                        // Étudiants — modifications admin uniquement
                        .requestMatchers(HttpMethod.PUT, "/users/students/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN_SCHOLAR")

                        // Enseignants — lecture pour admins et enseignants
                        .requestMatchers(HttpMethod.GET, "/users/teachers/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN_SCHOLAR", "TEACHER")

                        // Enseignants — création/modification admin uniquement
                        .requestMatchers(HttpMethod.POST, "/users/teachers/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN_SCHOLAR")
                        .requestMatchers(HttpMethod.PUT, "/users/teachers/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN_SCHOLAR")

                        // Personnel administratif
                        .requestMatchers(HttpMethod.POST, "/users/staff/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN_RH")
                        .requestMatchers(HttpMethod.GET, "/users/staff/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN_RH", "ADMIN_SCHOLAR")

                        // Recherche
                        .requestMatchers("/users/search/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN_SCHOLAR", "TEACHER")

                        // Tout le reste — authentifié
                        .anyRequest().authenticated()
                )
                .addFilterBefore(userSecurityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}