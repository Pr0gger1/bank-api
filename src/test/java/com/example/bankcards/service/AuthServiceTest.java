package com.example.bankcards.service;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.entity.AuthToken;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import com.example.bankcards.enums.TokenType;
import com.example.bankcards.repository.AuthTokenRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.AuthService;
import com.example.bankcards.security.JwtResponse;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
	
	@Mock
	private JwtService jwtService;
	@Mock private UserService userService;
	@Mock private AuthenticationManager authenticationManager;
	@Mock private UserRepository userRepository;
	@Mock private AuthTokenRepository authTokenRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private TokenBlacklistService tokenBlacklistService;
	
	@InjectMocks
	private AuthService authService;
	
	private User testUser;
	
	@BeforeEach
	void setUp() {
		testUser = User.builder()
				.id(UUID.randomUUID())
				.email("user@example.com")
				.password("password123")
				.role(Role.USER)
				.build();
	}
	
	@Test
	void login_ShouldReturnTokens_WhenCredentialsValid() {
		LoginRequest request = LoginRequest.builder()
				.email("user@example.com")
				.password("password123")
				.build();
		
		when(userRepository.findByEmail("user@example.com"))
				.thenReturn(Optional.of(testUser));
		
		when(jwtService.generateAccessToken(any())).thenReturn("access-token");
		when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
		
		JwtResponse response = authService.login(request);
		
		assertEquals("access-token", response.getAccessToken());
		assertEquals("refresh-token", response.getRefreshToken());
		verify(authTokenRepository).save(any(AuthToken.class));
	}
	
	@Test
	void login_ShouldThrowUnauthorized_WhenBadCredentials() {
		LoginRequest request = LoginRequest.builder()
				.email("user@example.com")
				.password("wrong")
				.build();
		
		doThrow(new BadCredentialsException("Bad credentials"))
				.when(authenticationManager)
				.authenticate(any());
		
		assertThrows(ResponseStatusException.class, () -> authService.login(request));
	}
	
	@Test
	void login_ShouldThrowUnauthorized_WhenUserNotFound() {
		LoginRequest request = LoginRequest.builder()
				.email("notfound@example.com")
				.password("password")
				.build();
		
		when(userRepository.findByEmail("notfound@example.com"))
				.thenReturn(Optional.empty());
		
		assertThrows(ResponseStatusException.class, () -> authService.login(request));
	}
	
	@Test
	void refreshToken_ShouldReturnNewTokens_WhenRefreshTokenValid() {
		AuthToken authToken = AuthToken.builder()
				.user(testUser)
				.token("refresh-token")
				.expirationDate(Instant.now().plus(5, ChronoUnit.MINUTES))
				.tokenType(TokenType.REFRESH)
				.build();
		
		when(jwtService.verifyRefreshToken("refresh-token"))
				.thenReturn(authToken);
		when(jwtService.validateRefreshToken("refresh-token"))
				.thenReturn(true);

		when(userRepository.findByEmail(testUser.getEmail()))
				.thenReturn(Optional.of(testUser));
		when(jwtService.generateAccessToken(testUser)).thenReturn("new-access");
		when(jwtService.generateRefreshToken(testUser)).thenReturn("new-refresh");
		
		JwtResponse response = authService.refreshToken("refresh-token");
		
		assertEquals("new-access", response.getAccessToken());
		assertEquals("new-refresh", response.getRefreshToken());
		verify(authTokenRepository).save(authToken);
	}
	
	@Test
	void refreshToken_ShouldThrowUnauthorized_WhenRefreshTokenExpired() {
		AuthToken expiredToken = AuthToken.builder()
				.user(testUser)
				.token("expired-token")
				.expirationDate(Instant.now().minus(1, ChronoUnit.MINUTES))
				.tokenType(TokenType.REFRESH)
				.build();
		
		when(jwtService.verifyRefreshToken("expired-token"))
				.thenReturn(expiredToken);
		
		assertThrows(ResponseStatusException.class,
				() -> authService.refreshToken("expired-token"));
	}
	
	@Test
	void logout_ShouldRevokeTokens_WhenValidAccessToken() {
		String accessToken = "valid-token";
		String authHeader = "Bearer " + accessToken;
		
		when(jwtService.validateAccessToken(accessToken)).thenReturn(true);
		when(jwtService.extractUsername(accessToken)).thenReturn("user@example.com");
		when(userRepository.findByEmail("user@example.com"))
				.thenReturn(Optional.of(testUser));
		
		AuthToken refreshToken = AuthToken.builder()
				.user(testUser)
				.token("refresh-token")
				.build();
		
		when(authTokenRepository.findByUser(testUser))
				.thenReturn(Optional.of(refreshToken));
		
		authService.logout(authHeader);
		
		assertTrue(refreshToken.isExpired());
		assertTrue(refreshToken.isRevoked());
		verify(tokenBlacklistService).blackListJwt(accessToken);
		verify(authTokenRepository).save(refreshToken);
	}
	
	@Test
	void logout_ShouldThrowUnauthorized_WhenAccessTokenInvalid() {
		String authHeader = "Bearer invalid";
		
		when(jwtService.validateAccessToken("invalid"))
				.thenReturn(false);
		
		assertThrows(ResponseStatusException.class,
				() -> authService.logout(authHeader));
	}
	
	@Test
	void logout_ShouldThrowUnauthorized_WhenUserNotFound() {
		String accessToken = "valid-token";
		String authHeader = "Bearer " + accessToken;
		
		when(jwtService.validateAccessToken(accessToken)).thenReturn(true);
		when(jwtService.extractUsername(accessToken)).thenReturn("unknown@example.com");
		when(userRepository.findByEmail("unknown@example.com"))
				.thenReturn(Optional.empty());
		
		assertThrows(ResponseStatusException.class,
				() -> authService.logout(authHeader));
	}
	
	@Test
	void register_ShouldSaveUserAndReturnTokens_WhenUserNotExists() {
		RegisterRequest request = RegisterRequest.builder()
				.email("new@example.com")
				.password("password123")
				.firstName("John")
				.lastName("Doe")
				.build();
		
		when(userService.checkUserExistence("new@example.com")).thenReturn(false);
		when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-pass");
		when(userRepository.save(any(User.class))).thenReturn(testUser);
		when(jwtService.generateAccessToken(any())).thenReturn("access");
		when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
		when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
		
		JwtResponse response = authService.register(request);
		
		assertEquals("access", response.getAccessToken());
		assertEquals("refresh", response.getRefreshToken());
	}
	
	@Test
	void register_ShouldThrowBadRequest_WhenUserAlreadyExists() {
		RegisterRequest request = RegisterRequest.builder()
				.email("user@example.com")
				.password("password123")
				.build();
		
		when(userService.checkUserExistence("user@example.com"))
				.thenReturn(true);
		
		assertThrows(ResponseStatusException.class,
				() -> authService.register(request));
	}
	
	@Test
	void register_ShouldThrowServerError_WhenRepositoryFails() {
		RegisterRequest request = RegisterRequest.builder()
				.email("fail@example.com")
				.password("password123")
				.build();
		
		when(userService.checkUserExistence("fail@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded");
		doThrow(new InvalidDataAccessApiUsageException("DB fail"))
				.when(userRepository).save(any(User.class));
		
		assertThrows(ResponseStatusException.class,
				() -> authService.register(request));
	}
}
