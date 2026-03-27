package com.mejbri.pfe.netopssynchro.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    public static final String USER_ID_HEADER = "Authorization";
    @Bean
    public OpenAPI customOpenAPI() {
        SecurityScheme userIdHeaderScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(USER_ID_HEADER)
                .description("Entrez un token JWT");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Authorization");

        return new OpenAPI()
                .info(new Info().title("RJIHM API").version("1.0"))
                .addSecurityItem(securityRequirement)
                .components(new Components()
                        .addSecuritySchemes("Authorization", userIdHeaderScheme));
    }


}