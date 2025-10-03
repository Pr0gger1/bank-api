package com.example.bankcards.controller;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.JwtResponse;
import com.example.bankcards.dto.response.LogoutResponse;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.TokenBlacklistService;
import com.example.bankcards.service.RefreshTokenService;
import com.example.bankcards.service.UserService;
import com.example.bankcards.service.impl.AuthService;
import com.example.bankcards.service.impl.JwtCookieService;
import com.example.bankcards.service.impl.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	@Autowired
	private MockMvc mockMvc;
	
	@MockitoBean
	private AuthService authService;
	
	@MockitoBean
	private UserRepository userRepository;
	
	@MockitoBean
	private JwtService jwtService;
	
	@MockitoBean
	private UserService userService;
	
	@MockitoBean
	private TokenBlacklistService blacklistService;
	
	@MockitoBean
	private RefreshTokenService refreshTokenService;
	
	@MockitoBean
	private JwtCookieService jwtCookieService;
	
	@Test
	public void testRegister_positive() throws Exception {
		String email = "email@example.com";
		RegisterRequest dto = RegisterRequest.builder()
				.email(email)
				.firstName("firstName")
				.lastName("lastName")
				.patronymic("patronymic")
				.password("password")
				.build();
		
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "mockRefreshToken")
				.httpOnly(true)
				.secure(false)
				.path("/")
				.maxAge(86400)
				.build();
		
		JwtResponse jwtResponse = JwtResponse.builder()
				.accessToken("mockAccessToken")
				.refreshCookie(refreshCookie)
				.build();
		
		when(userService.checkUserExistence(email)).thenReturn(false);
		when(authService.register(dto)).thenReturn(jwtResponse);
		
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.token").value("mockAccessToken"))
				.andExpect(header().exists(HttpHeaders.SET_COOKIE));
	}
	
	@Test
	public void testRegister_negative_userAlreadyExists() throws Exception {
		String email = "email@example.com";
		RegisterRequest dto = RegisterRequest.builder()
				.email(email)
				.firstName("firstName")
				.lastName("lastName")
				.patronymic("patronymic")
				.password("password")
				.build();
		
		when(userService.checkUserExistence(email)).thenReturn(true);
		when(authService.register(dto)).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists"));
		
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	public void testRegister_negative_invalidDto() throws Exception {
		RegisterRequest dto = RegisterRequest.builder()
				.email("invalid-email")
				.firstName("")
				.password("")
				.build();
		
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	public void testLogin_positive() throws Exception {
		String email = "email@example.com";
		String password = "password";
		LoginRequest dto = LoginRequest.builder()
				.email(email)
				.password(password)
				.build();
		
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "mockRefreshToken")
				.httpOnly(true)
				.secure(false)
				.path("/")
				.maxAge(86400)
				.build();
		
		JwtResponse jwtResponse = JwtResponse.builder()
				.accessToken("mockAccessToken")
				.refreshCookie(refreshCookie)
				.build();
		
		when(authService.login(dto)).thenReturn(jwtResponse);
		
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.token").value("mockAccessToken"))
				.andExpect(header().exists(HttpHeaders.SET_COOKIE));
	}
	
	@Test
	public void testLogin_negative_wrongCredentials() throws Exception {
		String email = "email@example.com";
		String password = "password";
		LoginRequest dto = LoginRequest.builder()
				.email(email)
				.password(password)
				.build();
		
		when(authService.login(dto)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
		
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testLogin_negative_invalidDto() throws Exception {
		LoginRequest dto = LoginRequest.builder()
				.email("invalid-email")
				.password("")
				.build();
		
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	public void testLogout_positive() throws Exception {
		String authHeader = "Bearer mockAccessToken";
		
		ResponseCookie cleanCookie = ResponseCookie.from("refreshToken", "")
				.httpOnly(true)
				.secure(false)
				.path("/")
				.maxAge(0)
				.build();
		
		LogoutResponse logoutResponse = LogoutResponse.builder()
				.refreshToken(cleanCookie)
				.build();
		
		when(authService.logout(any(HttpServletRequest.class), eq(authHeader))).thenReturn(logoutResponse);
		
		mockMvc.perform(post("/api/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, authHeader)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent())
				.andExpect(header().exists(HttpHeaders.SET_COOKIE));
	}
	
	@Test
	public void testLogout_negative_invalidAuthHeader() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
						// no authorization header
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	public void testLogout_negative_invalidToken() throws Exception {
		String authHeader = "Bearer invalidToken";
		
		when(authService.logout(any(HttpServletRequest.class), eq(authHeader)))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token is invalid"));
		
		mockMvc.perform(post("/api/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, authHeader)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testLogout_negative_malformedToken() throws Exception {
		String authHeader = "InvalidTokenFormat";
		
		when(authService.logout(any(HttpServletRequest.class), eq(authHeader)))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token format"));
		
		mockMvc.perform(post("/api/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, authHeader)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testRefreshToken_positive() throws Exception {
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "newMockRefreshToken")
				.httpOnly(true)
				.secure(false)
				.path("/")
				.maxAge(86400)
				.build();
		
		JwtResponse jwtResponse = JwtResponse.builder()
				.accessToken("newMockAccessToken")
				.refreshCookie(refreshCookie)
				.build();
		
		when(authService.refreshToken(any(HttpServletRequest.class))).thenReturn(jwtResponse);
		
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.cookie(new jakarta.servlet.http.Cookie("refreshToken", "oldMockRefreshToken")))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.token").value("newMockAccessToken"))
				.andExpect(header().exists(HttpHeaders.SET_COOKIE));
	}
	
	@Test
	public void testRefreshToken_negative_invalidRefreshToken() throws Exception {
		when(authService.refreshToken(any(HttpServletRequest.class)))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found"));
		
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testRefreshToken_negative_expiredRefreshToken() throws Exception {
		when(authService.refreshToken(any(HttpServletRequest.class)))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired"));
		
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.cookie(new jakarta.servlet.http.Cookie("refreshToken", "expiredToken")))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testRefreshToken_negative_blacklistedRefreshToken() throws Exception {
		when(authService.refreshToken(any(HttpServletRequest.class)))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is blacklisted"));
		
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.cookie(new jakarta.servlet.http.Cookie("refreshToken", "blacklistedToken")))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testRegister_verifyCookieProperties() throws Exception {
		String email = "email@example.com";
		RegisterRequest dto = RegisterRequest.builder()
				.email(email)
				.firstName("firstName")
				.lastName("lastName")
				.patronymic("patronymic")
				.password("password")
				.build();
		
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "mockRefreshToken")
				.httpOnly(true)
				.secure(true)
				.path("/api/auth/refresh")
				.maxAge(86400)
				.sameSite("Strict")
				.build();
		
		JwtResponse jwtResponse = JwtResponse.builder()
				.accessToken("mockAccessToken")
				.refreshCookie(refreshCookie)
				.build();
		
		when(userService.checkUserExistence(email)).thenReturn(false);
		when(authService.register(dto)).thenReturn(jwtResponse);
		
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isOk())
				.andExpect(header().exists(HttpHeaders.SET_COOKIE))
				.andExpect(cookie().exists("refreshToken"))
				.andExpect(cookie().value("refreshToken", "mockRefreshToken"))
				.andExpect(cookie().httpOnly("refreshToken", true))
				.andExpect(cookie().secure("refreshToken", true))
				.andExpect(cookie().path("refreshToken", "/api/auth/refresh"))
				.andExpect(cookie().maxAge("refreshToken", 86400));
	}
	
	@Test
	public void testLogout_verifyCleanCookie() throws Exception {
		String authHeader = "Bearer mockAccessToken";
		
		ResponseCookie cleanCookie = ResponseCookie.from("refreshToken", "")
				.httpOnly(true)
				.secure(false)
				.path("/")
				.maxAge(0)
				.build();
		
		LogoutResponse logoutResponse = LogoutResponse.builder()
				.refreshToken(cleanCookie)
				.build();
		
		when(authService.logout(any(HttpServletRequest.class), eq(authHeader))).thenReturn(logoutResponse);
		
		mockMvc.perform(post("/api/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, authHeader))
				.andExpect(status().isNoContent())
				.andExpect(header().string(HttpHeaders.SET_COOKIE,
						"refreshToken=; Path=/; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; HttpOnly"));
	}
}