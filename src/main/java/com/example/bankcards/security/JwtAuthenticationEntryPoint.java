package com.example.bankcards.security;

import com.example.bankcards.util.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
		int responseStatus = response.getStatus();
		log.error("Responding with unauthorized error. Message - {}", authException.getMessage());
		response.sendError(responseStatus, Constants.UNAUTHORIZED_ERROR_MESSAGE);
	}
}