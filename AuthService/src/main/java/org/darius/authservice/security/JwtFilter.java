package org.darius.authservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.darius.authservice.entities.Jwt;
import org.darius.authservice.repositories.JwtRepository;
import org.darius.authservice.services.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@RequiredArgsConstructor
@Service
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token;
        Jwt tokenRepository = null;
        String username = null;
        boolean isTokenExpired = true;

        try {
            final String authorizationHeader = request.getHeader("Authorization");
            if ( authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {

                token = authorizationHeader.substring(7);

                tokenRepository = this.jwtService.tokenByValue(token);

                isTokenExpired = this.jwtService.isTokenExpired(token);

                username = this.jwtService.extractUsername(token);
            }

            if (!isTokenExpired
                    && tokenRepository.getUser().getEmail().equals(username)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userService.findByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }
}
