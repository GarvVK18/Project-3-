package com.iam.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Identity and Access Management (IAM) Server API")
                        .version("1.0.0")
                        .description("Production-grade centralized Authentication & Authorization Server supporting " +
                                     "OAuth 2.0, OpenID Connect (OIDC), Time-based OTP (TOTP/MFA), " +
                                     "Redis session caching, programmatic token revocation, comprehensive audit logging, " +
                                     "and token-bucket rate limiting.")
                        .contact(new Contact()
                                .name("Pranav Gandewar")
                                .email("pranavgandewar113@gmail.com"))
                        .license(new License().name("Apache 2.0").url("https://spring.io/")))
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
