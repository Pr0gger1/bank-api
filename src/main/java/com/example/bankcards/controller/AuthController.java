package com.example.bankcards.controller;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RefreshTokenRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.security.AuthService;
import com.example.bankcards.security.JwtResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
	private final AuthService authService;

	@PostMapping("register")
	public ResponseEntity<JwtResponse> register(
		@RequestBody
		@Valid
		RegisterRequest request
	) {
		log.info("Registering user: {}", request);
		return ResponseEntity.ok(authService.register(request));
	}
	
	@PostMapping("login")
	public ResponseEntity<JwtResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
		log.info("Logging in user: {}", loginRequest);
		
		return ResponseEntity.ok(authService.login(loginRequest));
	}
	
	@PostMapping("logout")
	public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
		authService.logout(authHeader);
		return ResponseEntity.noContent().build();
	}
	
	@PostMapping("refresh")
	public ResponseEntity<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
		log.info("Refreshing token: {}", request);
		
		return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
	}
}
