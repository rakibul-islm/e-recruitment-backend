package com.bd.erecruitment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CORSConfig implements WebMvcConfigurer {

	// Guest-session tracking relies on a Set-Cookie round-trip, which browsers only honor
	// cross-origin when the CORS response carries a concrete allowed origin plus
	// allowCredentials(true) — a wildcard origin can't be paired with credentials.
	@Value("${app.frontend.base-url:http://localhost:4200}")
	private String frontendBaseUrl;

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {

			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
						.allowedMethods("GET", "POST", "PUT", "DELETE")
						.allowedHeaders("*")
						.allowedOriginPatterns(frontendBaseUrl)
						.allowCredentials(true);
			}
		};
	}
}
