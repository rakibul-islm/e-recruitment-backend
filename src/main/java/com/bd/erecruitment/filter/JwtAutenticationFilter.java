package com.bd.erecruitment.filter;

import com.bd.erecruitment.service.UserSessionService;
import com.bd.erecruitment.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAutenticationFilter extends OncePerRequestFilter {

	public static final String JWT_ERROR_ATTRIBUTE = "jwt_error";

	private final UserDetailsService userDetailsService;
	private final JwtUtil jwtUtil;
	private final UserSessionService userSessionService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		final String authorizationHeader = request.getHeader("Authorization");

		String username = null;
		String jwt = null;

		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			jwt = authorizationHeader.substring(7);
			try {
				username = jwtUtil.extractUsername(jwt);
			} catch (JwtException | IllegalArgumentException ex) {
				request.setAttribute(JWT_ERROR_ATTRIBUTE, ex.getMessage());
			}
		}

		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
			String jti = extractJtiSafely(jwt);
			boolean sessionRevoked = jti != null && !userSessionService.isActive(jti);
			if (!sessionRevoked && Boolean.TRUE.equals(jwtUtil.validateToken(jwt, userDetails))) {
				UsernamePasswordAuthenticationToken upat = new UsernamePasswordAuthenticationToken(userDetails, null,
						userDetails.getAuthorities());
				upat.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(upat);
			}
		}

		filterChain.doFilter(request, response);
	}

	// Tokens issued before session tracking existed carry no jti claim; treat those as always active.
	private String extractJtiSafely(String token) {
		try {
			return jwtUtil.extractJti(token);
		} catch (JwtException | IllegalArgumentException ex) {
			return null;
		}
	}

}
