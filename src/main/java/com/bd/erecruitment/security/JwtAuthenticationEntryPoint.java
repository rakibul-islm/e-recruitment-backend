package com.bd.erecruitment.security;

import com.bd.erecruitment.filter.JwtAutenticationFilter;
import com.bd.erecruitment.util.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
		Object jwtError = request.getAttribute(JwtAutenticationFilter.JWT_ERROR_ATTRIBUTE);
		String message = jwtError != null && StringUtils.isNotBlank(jwtError.toString())
				? jwtError.toString()
				: authException.getMessage();

		Response<Object> body = new Response<>();
		body.setCode(HttpServletResponse.SC_UNAUTHORIZED);
		body.setSuccess(false);
		body.setMessage(message);

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
