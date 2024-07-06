package com.bd.erecruitment.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "e-recruitment",
				version = "1.0.0",
				description = "E-Recruitment API Interface"
		),
		security = @SecurityRequirement(name = "Authorization")
)
@SecurityScheme(
		name = "Authorization",
		type = SecuritySchemeType.APIKEY,
		in = SecuritySchemeIn.HEADER,
		bearerFormat = "JWT",
		scheme = "bearer"
)
public class SwaggerConfig{

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.components(new Components()
						.addSecuritySchemes("Authorization", new io.swagger.v3.oas.models.security.SecurityScheme()
								.name("Authorization")
								.type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
						))
				.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("Authorization"));
	}
}
