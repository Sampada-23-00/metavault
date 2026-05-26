package com.sampada.metavault.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI/Swagger documentation.
 * springdoc-openapi reads your @RestController classes automatically,
 * but this config adds the project metadata and the JWT auth scheme.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI metavaultOpenAPI() {
        // Define a "bearer token" security scheme named "bearerAuth"
        // This tells Swagger UI to show an "Authorize" button where you paste your JWT.
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("MetaVault API")
                        .description("Metadata Backup & Version Control Service — " +
                                "track, diff, and restore versioned JSON configurations")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Sampada")
                                .email("sampada@example.com")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
