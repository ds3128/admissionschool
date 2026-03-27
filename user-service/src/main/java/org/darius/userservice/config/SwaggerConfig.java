package org.darius.userservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API")
                        .description("Gestion des profils, filières et structures académiques - AdmissionSchool")
                        .version("1.0.0")
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8888")
                                .description("API Gateway"),
                        new Server()
                                .url("http://localhost:8082")
                                .description("User Service")
                ));
//                .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
//                .components(new Components()
//                        .addSecuritySchemes("Bearer Auth", new SecurityScheme()
//                                .name("Bearer Auth")
//                                .type(SecurityScheme.Type.HTTP)
//                                .scheme("bearer")
//                                .bearerFormat("JWT")
//                        )
//                );
    }
}
