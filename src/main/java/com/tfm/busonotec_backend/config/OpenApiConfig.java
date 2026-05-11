package com.tfm.busonotec_backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Busonotec Backend API",
        version = "1.0.0",
        description = "API for dynamic business entities, fields, and physical business records."
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local development")
    }
)
public class OpenApiConfig {
}
