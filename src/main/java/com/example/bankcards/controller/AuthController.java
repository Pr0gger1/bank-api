package com.example.bankcards.controller;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.AuthorizationResponse;
import com.example.bankcards.dto.response.JwtResponse;
import com.example.bankcards.dto.response.LogoutResponse;
import com.example.bankcards.service.impl.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
	private final AuthService authService;

	@PostMapping("register")
	public ResponseEntity<AuthorizationResponse> register(
		@RequestBody
		@Valid
		RegisterRequest request
	) {
		log.info("Registering user: {}", request);
		JwtResponse response = authService.register(request);
		String accessToken = response.getAccessToken();
		ResponseCookie refreshCookie = response.getRefreshCookie();
		
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
				.body(new AuthorizationResponse(accessToken));
	}
	
	@PostMapping("login")
	public ResponseEntity<AuthorizationResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
		log.info("Logging in user: {}", loginRequest);
		JwtResponse response = authService.login(loginRequest);
		String accessCookie = response.getAccessToken();
		ResponseCookie refreshCookie = response.getRefreshCookie();
		
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
				.body(new AuthorizationResponse(accessCookie));
	}
	
	@PostMapping("logout")
	public ResponseEntity<Void> logout(
		HttpServletRequest request,
   		@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
	) {
		LogoutResponse response = authService.logout(request, authHeader);
		
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, response.getRefreshToken().toString())
				.build();
	}
	
	@PostMapping("refresh")
	public ResponseEntity<AuthorizationResponse> refreshToken(HttpServletRequest request) {
		log.info("Refreshing token: {}", request);
		
		JwtResponse response = authService.refreshToken(request);
		ResponseCookie refreshCookie = response.getRefreshCookie();
		
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
				.body(new AuthorizationResponse(response.getAccessToken()));
	}
}
