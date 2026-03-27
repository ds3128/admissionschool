package org.darius.gateway.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

@RestController
@Slf4j
public class ProxyController {

    private final RestClient restClient;

    public ProxyController() {
        this.restClient = RestClient.create();
    }

    private static final Map<String, String> ROUTES = Map.of(
            "/auth",           "http://localhost:8081",
            "/users",          "http://localhost:8082",
            "/admissions",     "http://localhost:8084",
            "/courses",        "http://localhost:8083",
            "/rh",             "http://localhost:8085",
            "/payments",       "http://localhost:8086",
            "/notifications",  "http://localhost:8087",
            "/documents",      "http://localhost:8089"
    );

    private static final Map<String, String> SWAGGER_ROUTES = Map.of(
            "/auth-service",       "http://localhost:8081",
            "/user-service",       "http://localhost:8082",
            "/admission-service",  "http://localhost:8084",
            "/course-service",     "http://localhost:8083",
            "/payment-service",    "http://localhost:8086",
            "/notification-service",     "http://localhost:8087"
    );

    @RequestMapping(value = {
            "/auth/**", "/users/**", "/admissions/**", "/courses/**",
            "/rh/**", "/payments/**", "/notifications/**", "/documents/**",
            "/auth-service/**", "/user-service/**",
            "/admission-service/**", "/course-service/**"
    })
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {

        String requestUri = request.getRequestURI();
        String targetBase = null;
        String targetPath = requestUri;

        // Swagger routes — strip du préfixe
        for (Map.Entry<String, String> entry : SWAGGER_ROUTES.entrySet()) {
            if (requestUri.startsWith(entry.getKey())) {
                targetBase = entry.getValue();
                targetPath = requestUri.substring(entry.getKey().length());
                if (targetPath.isEmpty()) targetPath = "/";
                break;
            }
        }

        // Routes métier
        if (targetBase == null) {
            for (Map.Entry<String, String> entry : ROUTES.entrySet()) {
                if (requestUri.startsWith(entry.getKey())) {
                    targetBase = entry.getValue();
                    targetPath = requestUri;
                    break;
                }
            }
        }

        if (targetBase == null) {
            log.warn("Aucune route pour : {}", requestUri);
            return ResponseEntity.notFound().build();
        }

        if (targetPath.endsWith("/") && targetPath.length() > 1) {
            targetPath = targetPath.substring(0, targetPath.length() - 1);
        }

        String targetUrl = targetBase + targetPath;

        if (request.getQueryString() != null) {
            targetUrl += "?" + request.getQueryString();
        }

        log.info("Proxy : {} {} → {}", request.getMethod(), requestUri, targetUrl);

        // ── Copier les headers ────────────────────────────────────────────────
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (!name.equalsIgnoreCase("host")
                    && !name.equalsIgnoreCase("content-length")) {  // ← exclure content-length
                headers.set(name, request.getHeader(name));
            }
        }

        // ── Lire le body en UTF-8 explicite ──────────────────────────────────
        String bodyStr = StreamUtils.copyToString(
                request.getInputStream(),
                java.nio.charset.StandardCharsets.UTF_8
        );

        // ── Forcer Content-Type avec charset UTF-8 ────────────────────────────
        if (headers.getContentType() == null
                || headers.getContentType().toString().contains("application/json")) {
            headers.setContentType(
                    new MediaType("application", "json", java.nio.charset.StandardCharsets.UTF_8)
            );
        }

        try {
            HttpEntity<String> entity = new HttpEntity<>(
                    bodyStr.isEmpty() ? null : bodyStr,
                    headers
            );

            return restClient
                    .method(HttpMethod.valueOf(request.getMethod()))
                    .uri(targetUrl)
                    .headers(h -> h.addAll(headers))
                    .body(bodyStr.isEmpty() ? "" : bodyStr)
                    .retrieve()
                    .toEntity(byte[].class);

        } catch (Exception ex) {
            log.error("Erreur proxy vers {} : {}", targetUrl, ex.getMessage());
            if (ex instanceof org.springframework.web.client.HttpStatusCodeException statusEx) {
                return ResponseEntity
                        .status(statusEx.getStatusCode())
                        .body(statusEx.getResponseBodyAsByteArray());
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}