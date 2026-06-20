package com.suraj.banking.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking Auth API")
                        .description(
                                "Production-grade Banking REST API — JWT Authentication, " +
                                "Spring Security 5, Role-Based Access Control (USER / MANAGER / ADMIN), " +
                                "and centralised exception handling via @ControllerAdvice."
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Suraj Prasad")
                                .email("suraj.suryn@gmail.com")))
                // Adds the JWT Bearer field to every endpoint in Swagger UI
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
