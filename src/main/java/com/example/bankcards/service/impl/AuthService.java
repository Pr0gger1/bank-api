package com.example.bankcards.service.impl;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.JwtResponse;
import com.example.bankcards.dto.response.LogoutResponse;
import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import com.example.bankcards.repository.RefreshTokenRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.TokenBlacklistService;
import com.example.bankcards.service.RefreshTokenService;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.Constants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {
	private final JwtService jwtService;
	private final UserService userService;
	private final TokenBlacklistService tokenBlacklistService;
	private final RefreshTokenService refreshTokenService;
	private final JwtCookieService jwtCookieService;
	
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	
	private final AuthenticationManager authenticationManager;
	private final PasswordEncoder passwordEncoder;
	
	public JwtResponse login(LoginRequest loginRequest) {
		String email = loginRequest.getEmail();
		String password = loginRequest.getPassword();
		
		return authenticate(email, password);
	}
	
	private JwtResponse authenticate(
			String email,
			String password
	) {
		try {
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(email, password);
			authenticationManager.authenticate(token);
		}
		catch (BadCredentialsException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}
		
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		
		String accessToken = jwtService.generateAccessToken(user);
		RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user);
		ResponseCookie refreshTokenCookie = jwtService.generateRefreshJwtCookie(refreshToken.getToken());
		
		return JwtResponse.builder()
				.accessToken(accessToken)
				.refreshCookie(refreshTokenCookie)
				.user(user)
				.build();
	}
	
	public JwtResponse refreshToken(HttpServletRequest request) {
		String refreshToken = jwtCookieService.getRefreshJwtFromCookies(request);
		
		if (refreshToken == null || refreshToken.isEmpty() || !jwtService.validateRefreshToken(refreshToken)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found");
		}
		
		RefreshToken oldRefreshToken = refreshTokenService.verifyRefreshToken(refreshToken);
		User user = oldRefreshToken.getUser();
		
		refreshTokenRepository.delete(oldRefreshToken);
		
		String accessToken = jwtService.generateAccessToken(user);
		RefreshToken newRefreshToken = refreshTokenService.generateRefreshToken(user);
		ResponseCookie refreshTokenCookie = jwtService.generateRefreshJwtCookie(newRefreshToken.getToken());
		
		return JwtResponse.builder()
				.accessToken(accessToken)
				.refreshCookie(refreshTokenCookie)
				.user(user)
				.build();
	}
	
	public LogoutResponse logout(HttpServletRequest request, String authHeader) {
		String accessToken = authHeader.substring(Constants.BEARER_PREFIX.length()).trim();
		String refreshToken = jwtCookieService.getRefreshJwtFromCookies(request);

		if (!jwtService.validateAccessToken(accessToken)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token is invalid");
		}
		
		if (refreshToken == null || !jwtService.validateRefreshToken(refreshToken)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
		}
		
		Optional<RefreshToken> optionalRefreshToken = refreshTokenRepository.findByToken(refreshToken);
		
		optionalRefreshToken.ifPresent(token -> {
			if (!accessToken.isEmpty()) {
				tokenBlacklistService.blackListJwt(accessToken);
			}

			refreshTokenRepository.deleteById(token.getId());
		});
		
		return LogoutResponse.builder()
				.refreshToken(jwtCookieService.getCleanJwtRefreshCookie())
				.build();
	}
	
	public JwtResponse register(RegisterRequest request) {
		if (userService.checkUserExistence(request.getEmail())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists");
		}
		
		try {
			String encodedPassword = passwordEncoder.encode(request.getPassword());
			
			User user = User.builder()
					.email(request.getEmail())
					.firstName(request.getFirstName())
					.lastName(request.getLastName())
					.patronymic(request.getPatronymic())
					.password(encodedPassword)
					.role(Role.USER)
					.build();
			
			userRepository.save(user);
			
			log.debug("AuthService[register][1]: user has been saved");
			
			return authenticate(request.getEmail(), request.getPassword());
		}
		catch (InvalidDataAccessApiUsageException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error");
		}
		catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}
}
