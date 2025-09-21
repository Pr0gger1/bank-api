package com.example.bankcards.controller;

import com.example.bankcards.dto.RefreshTokenRequest;
import com.example.bankcards.dto.SignInRequest;
import com.example.bankcards.dto.UserDto;
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
		UserDto userDto
	) {
		log.info("Registering user: {}", userDto);
		return ResponseEntity.ok(authService.register(userDto));
	}
	
	@PostMapping("login")
	public ResponseEntity<JwtResponse> login(@RequestBody @Valid SignInRequest signInRequest) {
		log.info("Logging in user: {}", signInRequest);
		
		return ResponseEntity.ok(authService.login(signInRequest));
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
