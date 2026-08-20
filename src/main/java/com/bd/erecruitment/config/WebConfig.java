package com.bd.erecruitment.config;

import com.bd.erecruitment.security.GuestTrackingInterceptor;
import com.bd.erecruitment.security.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final PermissionInterceptor permissionInterceptor;
	private final GuestTrackingInterceptor guestTrackingInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(permissionInterceptor)
			.addPathPatterns("/**")
			.excludePathPatterns(
				"/authenticate/**",
				"/user/signup",
				"/job-circular/filter",
				"/h2-console/**",
				"/actuator/**",
				"/v3/api-docs/**",
				"/swagger-ui/**",
				"/swagger-ui.html"
			);

		registry.addInterceptor(guestTrackingInterceptor)
			.addPathPatterns("/**")
			.excludePathPatterns(
				"/h2-console/**",
				"/actuator/**",
				"/v3/api-docs/**",
				"/swagger-ui/**",
				"/swagger-ui.html"
			);
	}
}
