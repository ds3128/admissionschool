package org.darius.gateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtValidationService jwtValidationService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/");
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

        HttpServletRequestWrapper mutatedRequest = new HttpServletRequestWrapper(request) {

            @Override
            public String getHeader(String name) {
                if ("X-User-Email".equals(name)) return jwtValidationService.extractEmail(token);
                if ("X-User-Role".equals(name))  return jwtValidationService.extractRole(token);
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                names.add("X-User-Email");
                names.add("X-User-Role");
                return Collections.enumeration(names);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if ("X-User-Email".equals(name))
                    return Collections.enumeration(List.of(jwtValidationService.extractEmail(token)));
                if ("X-User-Role".equals(name))
                    return Collections.enumeration(List.of(jwtValidationService.extractRole(token)));
                return super.getHeaders(name);
            }
        };

        filterChain.doFilter(mutatedRequest, response);
    }
}
