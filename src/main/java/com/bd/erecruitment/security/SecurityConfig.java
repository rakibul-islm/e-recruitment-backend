package com.bd.erecruitment.security;

import com.bd.erecruitment.filter.JwtAutenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAutenticationFilter jwtRequestFilter;
	private final UserDetailsService userService;
	private final BCryptPasswordEncoder passwordEncoder;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> cors.configure(http)) // Enable CORS
				.csrf(csrf -> csrf.disable()) // Disable CSRF
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/authenticate/**",
								"/user/signup",
								"/job-circular/filter"
						).permitAll()
						.requestMatchers(
								"/actuator/**",
								"**/actuator/**"
						).permitAll()
						.requestMatchers(AUTH_WHITELIST).permitAll()
						.anyRequest().authenticated()
				)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	private static final String[] AUTH_WHITELIST = {
			"/api/v1/auth/**",
			"/v3/api-docs/**",
			"/v3/api-docs.yaml",
			"/swagger-ui.html",
			"/swagger-ui/**"
	};

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}
