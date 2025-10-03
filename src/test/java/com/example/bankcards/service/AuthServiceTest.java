package com.example.bankcards.service;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.JwtResponse;
import com.example.bankcards.dto.response.LogoutResponse;
import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import com.example.bankcards.enums.TokenType;
import com.example.bankcards.repository.RefreshTokenRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.TokenBlacklistService;
import com.example.bankcards.service.impl.AuthService;
import com.example.bankcards.service.impl.JwtCookieService;
import com.example.bankcards.service.impl.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
	
	@Mock
	private JwtService jwtService;
	
	@Mock
	private UserService userService;
	
	@Mock
	private TokenBlacklistService tokenBlacklistService;
	
	@Mock
	private RefreshTokenService refreshTokenService;
	
	@Mock
	private JwtCookieService jwtCookieService;
	
	@Mock
	private UserRepository userRepository;
	
	@Mock
	private RefreshTokenRepository refreshTokenRepository;
	
	@Mock
	private AuthenticationManager authenticationManager;
	
	@Mock
	private PasswordEncoder passwordEncoder;
	
	@Mock
	private HttpServletRequest httpServletRequest;
	
	@InjectMocks
	private AuthService authService;
	
	private User testUser;
	private RefreshToken testRefreshToken;
	
	@BeforeEach
	void setUp() {
		testUser = User.builder()
				.id(UUID.randomUUID())
				.email("user@example.com")
				.password("password123")
				.firstName("John")
				.lastName("Doe")
				.role(Role.USER)
				.build();
		
		testRefreshToken = RefreshToken.builder()
				.id(UUID.randomUUID())
				.token("refresh-token")
				.user(testUser)
				.expirationDate(Instant.now().plus(5, ChronoUnit.MINUTES))
				.tokenType(TokenType.REFRESH)
				.build();
	}
	
	@Test
	void login_ShouldReturnJwtResponse_WhenCredentialsValid() {
		
		LoginRequest request = LoginRequest.builder()
				.email("user@example.com")
				.password("password123")
				.build();
		
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "refresh-token")
				.httpOnly(true)
				.path("/api/auth/refresh")
				.maxAge(86400)
				.build();
		
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(null);
		when(userRepository.findByEmail("user@example.com"))
				.thenReturn(Optional.of(testUser));
		when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
		when(refreshTokenService.generateRefreshToken(testUser)).thenReturn(testRefreshToken);
		when(jwtService.generateRefreshJwtCookie("refresh-token")).thenReturn(refreshCookie);
		
		
		JwtResponse response = authService.login(request);
		
		
		assertNotNull(response);
		assertEquals("access-token", response.getAccessToken());
		assertEquals(refreshCookie, response.getRefreshCookie());
		assertEquals(testUser, response.getUser());
		
		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(userRepository).findByEmail("user@example.com");
		verify(jwtService).generateAccessToken(testUser);
		verify(refreshTokenService).generateRefreshToken(testUser);
		verify(jwtService).generateRefreshJwtCookie("refresh-token");
	}
	
	@Test
	void login_ShouldThrowUnauthorized_WhenBadCredentials() {
		
		LoginRequest request = LoginRequest.builder()
				.email("user@example.com")
				.password("wrong-password")
				.build();
		
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new BadCredentialsException("Bad credentials"));
		
		 
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> authService.login(request));
		
		assertEquals(401, exception.getStatusCode().value());
		assertEquals("Invalid email or password", exception.getReason());
		
		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(userRepository, never()).findByEmail(anyString());
	}
	
	@Test
	void login_ShouldThrowUnauthorized_WhenUserNotFound() {
		
		LoginRequest request = LoginRequest.builder()
				.email("notfound@example.com")
				.password("password123")
				.build();
		
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(null);
		when(userRepository.findByEmail("notfound@example.com"))
				.thenReturn(Optional.empty());
		
		 
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> authService.login(request));
		
		assertEquals(401, exception.getStatusCode().value());
		assertEquals("User not found", exception.getReason());
		
		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(userRepository).findByEmail("notfound@example.com");
	}
	
	@Test
	void refreshToken_ShouldReturnNewJwtResponse_WhenRefreshTokenValid() {
		
		String refreshTokenValue = "valid-refresh-token";
		RefreshToken newRefreshToken = RefreshToken.builder()
				.token("new-refresh-token")
				.user(testUser)
				.build();
		
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "new-refresh-token")
				.httpOnly(true)
				.path("/api/auth/refresh")
				.maxAge(86400)
				.build();
		
		when(jwtCookieService.getRefreshJwtFromCookies(httpServletRequest)).thenReturn(refreshTokenValue);
		when(jwtService.validateRefreshToken(refreshTokenValue)).thenReturn(true);
		when(refreshTokenService.verifyRefreshToken(refreshTokenValue)).thenReturn(testRefreshToken);
		when(jwtService.generateAccessToken(testUser)).thenReturn("new-access-token");
		when(refreshTokenService.generateRefreshToken(testUser)).thenReturn(newRefreshToken);
		when(jwtService.generateRefreshJwtCookie("new-refresh-token")).thenReturn(refreshCookie);
		
		
		JwtResponse response = authService.refreshToken(httpServletRequest);
		
		
		assertNotNull(response);
		assertEquals("new-access-token", response.getAccessToken());
		assertEquals(refreshCookie, response.getRefreshCookie());
		assertEquals(testUser, response.getUser());
		
		verify(jwtCookieService).getRefreshJwtFromCookies(httpServletRequest);
		verify(jwtService).validateRefreshToken(refreshTokenValue);
		verify(refreshTokenService).verifyRefreshToken(refreshTokenValue);
		verify(refreshTokenRepository).delete(testRefreshToken);
		verify(jwtService).generateAccessToken(testUser);
		verify(refreshTokenService).generateRefreshToken(testUser);
		verify(jwtService).generateRefreshJwtCookie("new-refresh-token");
	}
	
	@Test
	void refreshToken_ShouldThrowUnauthorized_WhenRefreshTokenNotFoundInCookies() {
		
		when(jwtCookieService.getRefreshJwtFromCookies(httpServletRequest)).thenReturn(null);
		
		 
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> authService.refreshToken(httpServletRequest));
		
		assertEquals(401, exception.getStatusCode().value());
		assertEquals("Refresh token not found", exception.getReason());
		
		verify(jwtCookieService).getRefreshJwtFromCookies(httpServletRequest);
		verify(jwtService, never()).validateRefreshToken(anyString());
	}
	
	@Test
	void refreshToken_ShouldThrowUnauthorized_WhenRefreshTokenInvalid() {
		
		String refreshTokenValue = "invalid-refresh-token";
		
		when(jwtCookieService.getRefreshJwtFromCookies(httpServletRequest)).thenReturn(refreshTokenValue);
		when(jwtService.validateRefreshToken(refreshTokenValue)).thenReturn(false);
		
		 
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> authService.refreshToken(httpServletRequest));
		
		assertEquals(401, exception.getStatusCode().value());
		assertEquals("Refresh token not found", exception.getReason());
		
		verify(jwtCookieService).getRefreshJwtFromCookies(httpServletRequest);
		verify(jwtService).validateRefreshToken(refreshTokenValue);
		verify(refreshTokenService, never()).verifyRefreshToken(anyString());
	}
	
	@Test
	void refreshToken_ShouldThrowUnauthorized_WhenRefreshTokenExpired() {
		
		String refreshTokenValue = "expired-refresh-token";
		
		RefreshToken expiredToken = RefreshToken.builder()
				.token(refreshTokenValue)
				.expirationDate(Instant.now().minus(1, ChronoUnit.MINUTES))
				.build();
		
		when(jwtCookieService.getRefreshJwtFromCookies(httpServletRequest)).thenReturn(refreshTokenValue);
		when(jwtService.validateRefreshToken(refreshTokenValue)).thenReturn(true);
		when(refreshTokenService.verifyRefreshToken(refreshTokenValue))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired"));
		
		 
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> authService.refreshToken(httpServletRequest));
		
		assertEquals(401, exception.getStatusCode().value());
		
		verify(jwtCookieService).getRefreshJwtFromCookies(httpServletRequest);
		verify(jwtService).validateRefreshToken(refreshTokenValue);
		verify(refreshTokenService).verifyRefreshToken(refreshTokenValue);
	}
	
	@Test
	void logout_ShouldReturnLogoutResponse_WhenTokensValid() {
		
		String authHeader = "Bearer access-token";
		String refreshTokenValue = "refresh-token";
		
		ResponseCookie cleanCookie = ResponseCookie.from("refreshToken", "")
				.path("/api/auth/refresh")
				.maxAge(0)
				.httpOnly(true)
				.build();
		
		when(jwtCookieService.getRefreshJwtFromCookies(httpServletRequest)).thenReturn(refreshTokenValue);
		when(jwtService.validateAccessToken("access-token")).thenReturn(true);
		when(jwtService.validateRefreshToken(refreshTokenValue)).thenReturn(true);
		when(refreshTokenRepository.findByToken(refreshTokenValue)).thenReturn(Optional.of(testRefreshToken));
		when(jwtCookieService.getCleanJwtRefreshCookie()).thenReturn(cleanCookie);
		
		
		LogoutResponse response = authService.logout(httpServletRequest, authHeader);
		
		
		assertNotNull(response);
		assertEquals(cleanCookie, response.getRefreshToken());
		
		verify(jwtCookieService).getRefreshJwtFromCookies(httpServletRequest);
		verify(jwtService).validateAccessToken("access-token");
		verify(jwtService).validateRefreshToken(refreshTokenValue);
		verify(refreshTokenRepository).findByToken(refreshTokenValue);
		verify(tokenBlacklistService).blackListJwt("access-token");
		verify(refreshTokenRepository).deleteById(testRefreshToken.getId());
		verify(jwtCookieService).getCleanJwtRefreshCookie();
	}
	
	@Test
	void logout_ShouldThrowUnauthorized_WhenAccessTokenInvalid() {
		
		String authHeader = "Bearer invalid-token";
		
		when(jwtService.validateAccessToken("invalid-token")).thenReturn(false);
		
		 
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> authService.logout(httpServletRequest, authHeader));
		
		assertEquals(401, exception.getStatusCode().value());
		assertEquals("Access token is invalid", exception.getReason());
		
		verify(jwtService).validateAccessToken("invalid-token");
	}
	
	@Test
	void logout_ShouldThrowUnauthorized_WhenRefreshTokenInvalid() {
		String authHeader = "Bearer access-token";
		
		when(jwtService.validateAccessToken("access-token")).thenReturn(true);
		when(jwtCookieService.getRefreshJwtFromCookies(httpServletRequest)).thenReturn("invalid-refresh-token");
		when(jwtService.validateRefreshToken("invalid-refresh-token")).thenReturn(false);
		
		 
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> authService.logout(httpServletRequest, authHeader));
		
		assertEquals(401, exception.getStatusCode().value());
		assertEquals("Refresh token is invalid", exception.getReason());
		
		verify(jwtService).validateAccessToken("access-token");
		verify(jwtCookieService).getRefreshJwtFromCookies(httpServletRequest);
		verify(jwtService).validateRefreshToken("invalid-refresh-token");
		verify(refreshTokenRepository, never()).findByToken(anyString());
	}
	
	@Test
	void logout_ShouldWork_WhenRefreshTokenNotFoundInRepository() {
		
		String authHeader = "Bearer access-token";
		String refreshTokenValue = "refresh-token";
		
		ResponseCookie cleanCookie = ResponseCookie.from("refreshToken", "")
				.path("/api/auth/refresh")
				.maxAge(0)
				.httpOnly(true)
				.build();
		
		when(jwtCookieService.getRefreshJwtFromCookies(httpServletRequest)).thenReturn(refreshTokenValue);
		when(jwtService.validateAccessToken("access-token")).thenReturn(true);
		when(jwtService.validateRefreshToken(refreshTokenValue)).thenReturn(true);
		when(refreshTokenRepository.findByToken(refreshTokenValue)).thenReturn(Optional.empty());
		when(jwtCookieService.getCleanJwtRefreshCookie()).thenReturn(cleanCookie);
		
		LogoutResponse response = authService.logout(httpServletRequest, authHeader);
		
		assertNotNull(response);
		assertEquals(cleanCookie, response.getRefreshToken());
		
		verify(jwtCookieService).getRefreshJwtFromCookies(httpServletRequest);
		verify(jwtService).validateAccessToken("access-token");
		verify(jwtService).validateRefreshToken(refreshTokenValue);
		verify(refreshTokenRepository).findByToken(refreshTokenValue);
		verify(tokenBlacklistService, never()).blackListJwt(anyString());
		verify(refreshTokenRepository, never()).deleteById(any());
		verify(jwtCookieService).getCleanJwtRefreshCookie();
	}
	
	@Test
	void register_ShouldSaveUserAndReturnJwtResponse_WhenUserNotExists() {
		RegisterRequest request = RegisterRequest.builder()
				.email("new@example.com")
				.password("password123")
				.firstName("John")
				.lastName("Doe")
				.patronymic("Smith")
				.build();
		
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "refresh-token")
				.httpOnly(true)
				.path("/api/auth/refresh")
				.maxAge(86400)
				.build();
		
		when(userService.checkUserExistence("new@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenReturn(testUser);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(null);
		when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(testUser));
		when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
		when(refreshTokenService.generateRefreshToken(testUser)).thenReturn(testRefreshToken);
		when(jwtService.generateRefreshJwtCookie("refresh-token")).thenReturn(refreshCookie);
		
		
		JwtResponse response = authService.register(request);
		
		
		assertNotNull(response);
		assertEquals("access-token", response.getAccessToken());
		assertEquals(refreshCookie, response.getRefreshCookie());
		assertEquals(testUser, response.getUser());
		
		verify(userService).checkUserExistence("new@example.com");
		verify(passwordEncoder).encode("password123");
		verify(userRepository).save(any(User.class));
		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(userRepository).findByEmail("new@example.com");
		verify(jwtService).generateAccessToken(testUser);
		verify(refreshTokenService).generateRefreshToken(testUser);
		verify(jwtService).generateRefreshJwtCookie("refresh-token");
	}
	
	@Test
	void register_ShouldThrowBadRequest_WhenUserAlreadyExists() {
		
		RegisterRequest request = RegisterRequest.builder()
				.email("existing@example.com")
				.password("password123")
				.build();
		
		when(userService.checkUserExistence("existing@example.com")).thenReturn(true);
		 
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> authService.register(request));
		
		assertEquals(400, exception.getStatusCode().value());
		assertEquals("User already exists", exception.getReason());
		
		verify(userService).checkUserExistence("existing@example.com");
		verify(userRepository, never()).save(any(User.class));
	}
	
	@Test
	void register_ShouldThrowInternalServerError_WhenRepositoryFails() {
		
		RegisterRequest request = RegisterRequest.builder()
				.email("fail@example.com")
				.password("password123")
				.firstName("John")
				.lastName("Doe")
				.build();
		
		when(userService.checkUserExistence("fail@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(userRepository.save(any(User.class)))
				.thenThrow(new InvalidDataAccessApiUsageException("Database error"));
		
		 
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> authService.register(request));
		
		assertEquals(500, exception.getStatusCode().value());
		assertEquals("Server Error", exception.getReason());
		
		verify(userService).checkUserExistence("fail@example.com");
		verify(passwordEncoder).encode("password123");
		verify(userRepository).save(any(User.class));
		verify(authenticationManager, never()).authenticate(any());
	}
	
	@Test
	void register_ShouldThrowBadRequest_WhenOtherExceptionOccurs() {
		
		RegisterRequest request = RegisterRequest.builder()
				.email("error@example.com")
				.password("password123")
				.build();
		
		when(userService.checkUserExistence("error@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(userRepository.save(any(User.class)))
				.thenThrow(new RuntimeException("Some other error"));
		
		 
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> authService.register(request));
		
		assertEquals(400, exception.getStatusCode().value());
		assertEquals("Some other error", exception.getReason());
		
		verify(userService).checkUserExistence("error@example.com");
		verify(passwordEncoder).encode("password123");
		verify(userRepository).save(any(User.class));
	}
}