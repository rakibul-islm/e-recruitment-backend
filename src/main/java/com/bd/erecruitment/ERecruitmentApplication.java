package com.bd.erecruitment;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@OpenAPIDefinition(info = @Info(title = "e-recruitment",version = "V.1.0",description = "Documentation for e-recruitment"))
@SpringBootApplication
public class ERecruitmentApplication {

	public static void main(String[] args) {
		SpringApplication.run(ERecruitmentApplication.class, args);
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
