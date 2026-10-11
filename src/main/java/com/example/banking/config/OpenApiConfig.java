package com.example.banking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger/OpenAPI setup: adds a JWT "Authorize" button to the Swagger UI so the documented
 * API can actually be tried with a real token. Flow: POST /api/auth/login, copy the token,
 * click "Authorize" in the UI, enter "Bearer &lt;token&gt;".
 *
 * The Swagger endpoints themselves (/swagger-ui.html, /v3/api-docs) are public - see
 * SecurityConfig - but every /api/** endpoint still requires the JWT.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bankingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SpringBank API")
                        .version("1.0")
                        .description("REST API for the SpringBank banking system: authentication, accounts,"
                                + " deposits, withdrawals, transfers and transaction history."))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
