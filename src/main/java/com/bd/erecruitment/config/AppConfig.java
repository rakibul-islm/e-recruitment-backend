package com.bd.erecruitment.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Data
@Service
public class AppConfig {
	
	@Value("${api.ami.user}")
	private String user;
	
	@Value("${api.ami.pass}")
	private String pass;
}
