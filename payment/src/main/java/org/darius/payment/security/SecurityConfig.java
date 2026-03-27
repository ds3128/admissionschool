package org.darius.payment.security;

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

    private final PaymentSecurityFilter paymentSecurityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                        .requestMatchers("/payments/webhook").permitAll()
                        // Frais de dossier — candidat
                        .requestMatchers("/payments/admission-fees")
                        .hasAnyRole("CANDIDATE", "STUDENT", "SUPER_ADMIN")
                        // Mes données
                        .requestMatchers("/payments/me", "/payments/invoices/me", "/payments/scholarships/me")
                        .hasAnyRole("STUDENT", "CANDIDATE", "SUPER_ADMIN")
                        // Paiement facture
                        .requestMatchers(HttpMethod.POST, "/payments/invoices/*/pay")
                        .hasAnyRole("STUDENT", "SUPER_ADMIN")
                        // Administration finance
                        .requestMatchers("/payments/invoices/generate", "/payments/invoices/*/schedule",
                                "/payments/invoices/*/cancel")
                        .hasAnyRole("ADMIN_FINANCE", "SUPER_ADMIN")
                        .requestMatchers("/payments/scholarships/**")
                        .hasAnyRole("ADMIN_FINANCE", "SUPER_ADMIN", "STUDENT")
                        .requestMatchers("/payments/admin/**")
                        .hasAnyRole("ADMIN_FINANCE", "SUPER_ADMIN")
                        .requestMatchers("/payments/*/refund")
                        .hasAnyRole("ADMIN_FINANCE", "SUPER_ADMIN")
                        .requestMatchers("/payments/*/simulate-confirm")
                        .hasAnyRole("ADMIN_FINANCE", "SUPER_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(paymentSecurityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
