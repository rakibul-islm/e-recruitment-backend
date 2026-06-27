package com.bd.erecruitment.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "e-recruitment",
				version = "1.0.0",
				description = "E-Recruitment API Interface"
		)
)
public class SwaggerConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.components(new Components()
						.addSecuritySchemes("oauth2Password", new SecurityScheme()
								.name("oauth2Password")
								.type(SecurityScheme.Type.OAUTH2)
								.description("Login with username and password")
								.flows(new OAuthFlows()
										.password(new OAuthFlow()
												.tokenUrl("/e-recruitment/authenticate/oauth2/token")
												.scopes(new Scopes())
										)
								)
						)
				)
				.addSecurityItem(new SecurityRequirement().addList("oauth2Password"));
	}
}
