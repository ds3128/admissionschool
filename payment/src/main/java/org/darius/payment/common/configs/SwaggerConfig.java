package org.darius.payment.common.configs;

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
                        .title("Payment Service API")
                        .description("Gestion des paiements, factures et bourses")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8086")
                                .description("Direct"),
                        new Server()
                                .url("http://localhost:8888")
                                .description("Via Gateway")
                ));
    }
}