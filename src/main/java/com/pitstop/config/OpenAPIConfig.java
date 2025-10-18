package com.pitstop.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for PitStop API documentation.
 *
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access API docs at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("PitStop API")
                .version("0.0.1-SNAPSHOT")
                .description("""
                    API REST para sistema de gerenciamento de oficina mecânica.

                    Funcionalidades:
                    - Gestão de clientes e veículos
                    - Ordens de serviço (OS)
                    - Controle de estoque de peças
                    - Gestão financeira e pagamentos
                    - Notificações em tempo real (WebSocket)

                    Autenticação: JWT Bearer Token (será implementado na Fase 1)
                    """)
                .contact(new Contact()
                    .name("PitStop Team")
                    .email("contato@pitstop.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Development Server"),
                new Server()
                    .url("https://api.pitstop.com")
                    .description("Production Server")
            ));
    }
}
