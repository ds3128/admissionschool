package org.darius.gateway.swagger;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@Slf4j
public class SwaggerDocsController {

    private final RestTemplate restTemplate;

    // Map service name → URL de base
    private static final Map<String, String> SERVICES = Map.of(
            "auth-service",        "http://localhost:8081",
            "user-service",        "http://localhost:8082",
            "course-service",      "http://localhost:8083",
            "admission-service",   "http://localhost:8084"
    );

    public SwaggerDocsController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/{serviceName}/v3/api-docs")
    public ResponseEntity<Object> getApiDocs(@PathVariable String serviceName) {
        String baseUrl = SERVICES.get(serviceName);

        if (baseUrl == null) {
            log.warn("Service inconnu : {}", serviceName);
            return ResponseEntity.notFound().build();
        }

        String targetUrl = baseUrl + "/v3/api-docs";
        log.info("Proxying Swagger docs : {} → {}", serviceName, targetUrl);

        try {
            Object docs = restTemplate.getForObject(targetUrl, Object.class);
            return ResponseEntity.ok(docs);
        } catch (Exception ex) {
            log.error("Erreur lors de la récupération des docs pour {} : {}", serviceName, ex.getMessage());
            return ResponseEntity.status(503).build();
        }
    }
}