package org.darius.gateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtValidationService jwtValidationService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path   = request.getRequestURI();
        String method = request.getMethod();

        // Routes publiques GET sans token
        if ("GET".equals(method) && (
                path.startsWith("/users/filieres")              ||
                        path.startsWith("/users/departments")           ||
                        path.startsWith("/admissions/campaigns")        ||
                        path.startsWith("/admissions/offers")           ||
                        path.startsWith("/admissions/required-documents")
        )) {
            return true;
        }

        if ("/payments/webhook".equals(path)) return true;

        return path.startsWith("/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/webjars/")
                || path.contains("/v3/api-docs")
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = authorizationHeader.substring(7);

        if (!jwtValidationService.isValidToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String email = jwtValidationService.extractEmail(token);
        String role  = jwtValidationService.extractRole(token);
        String userId = jwtValidationService.extractUserId(token);
        log.info(">>> Gateway injecte — X-User-Email: {}, X-User-Role: {}, X-User-Role: {}", email, role, userId);

        HttpServletRequestWrapper mutatedRequest = new HttpServletRequestWrapper(request) {

            @Override
            public String getHeader(String name) {
                if ("X-User-Email".equals(name)) return email;
                if ("X-User-Role".equals(name))  return role;
                if ("X-user-Id".equals(name)) return userId;
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                names.add("X-User-Email");
                names.add("X-User-Role");
                names.add("X-user-Id");
                return Collections.enumeration(names);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if ("X-User-Email".equals(name))
                    return Collections.enumeration(List.of(email));
                if ("X-User-Role".equals(name))
                    return Collections.enumeration(List.of(role));
                if ("X-user-Id".equals(name))
                    return Collections.enumeration(List.of(userId));
                return super.getHeaders(name);
            }
        };

        filterChain.doFilter(mutatedRequest, response);
    }
}
