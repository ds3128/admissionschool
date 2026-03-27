package org.darius.course.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
                        .title("Course Service API")
                        .description("Gestion académique : cours, présences, évaluations, progression")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8083")
                                .description("Direct"),
                        new Server()
                                .url("http://localhost:8888")
                                .description("Via Gateway")
                ));
    }
}
