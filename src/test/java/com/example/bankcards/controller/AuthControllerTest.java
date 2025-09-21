package com.example.bankcards.controller;


import com.example.bankcards.dto.RefreshTokenRequest;
import com.example.bankcards.dto.SignInRequest;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.AuthService;
import com.example.bankcards.security.JwtResponse;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.security.TokenBlacklistService;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

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
	
	@Test
	public void testRegister_positive() throws Exception {
		String email = "email@example.com";
		UserDto dto = UserDto.builder()
				.email(email)
				.firstName("firstName")
				.lastName("lastName")
				.patronymic("patronymic")
				.password("password")
				.build();
		
		JwtResponse jwtResponse = JwtResponse.builder()
				.accessToken("mockAccessToken")
				.refreshToken("mockRefreshToken")
				.build();
		
		when(userService.checkUserExistence(email)).thenReturn(false);
		when(authService.register(dto)).thenReturn(jwtResponse);
		
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(dto)))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.accessToken").value("mockAccessToken"));
	}
	
	
	@Test
	public void testRegister_negative_userAlreadyExists() throws Exception {
		String email = "email@example.com";
		UserDto dto = UserDto.builder()
				.email(email)
				.firstName("firstName")
				.lastName("lastName")
				.patronymic("patronymic")
				.password("password")
				.build();
		
		when(userService.checkUserExistence(email)).thenReturn(true);
		when(authService.register(dto)).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST));
		
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(dto)))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	public void testRegister_negative_invalidDto() throws Exception {
		UserDto dto = UserDto.builder()
				.email("email")
				.firstName("firstName")
				.patronymic("patronymic")
				.password("")
				.build();
		
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(dto)))
				.andExpect(status().isBadRequest());
	}
	
	
	@Test
	public void testLogin_positive() throws Exception {
		String email = "email@example.com";
		String password = "password";
		SignInRequest dto = SignInRequest.builder()
				.email(email)
				.password(password)
				.build();
		
		JwtResponse jwtResponse = JwtResponse.builder()
				.accessToken("mockAccessToken")
				.refreshToken("mockRefreshToken")
				.build();
		
		when(authService.login(dto)).thenReturn(jwtResponse);
		
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(dto)))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.accessToken").value("mockAccessToken"));
	}
	
	@Test
	public void testLogin_negative_wrongCredentials() throws Exception {
		String email = "email@example.com";
		String password = "password";
		SignInRequest dto = SignInRequest.builder()
				.email(email)
				.password(password)
				.build();
		
		when(authService.login(dto)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(dto)))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testLogin_negative_invalidDto() throws Exception {
		SignInRequest dto = SignInRequest.builder()
				.email("email")
				.password("")
				.build();
		
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(dto)))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	public void testLogout_positive() throws Exception {
		String authHeader = "Bearer mockAccessToken";
		
		mockMvc.perform(post("/api/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, authHeader)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent());
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
	public void testLogout_negative_userNotFound() throws Exception {
		String authHeader = "Bearer mockAccessToken";
		
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED)).when(authService).logout(authHeader);
		
		mockMvc.perform(post("/api/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, authHeader)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testLogout_negative_malformedToken() throws Exception {
		String authHeader = "Bearer invalid token";
		String accessToken = authHeader.substring(Constants.BEARER_PREFIX.length()).trim();
		
		when(jwtService.validateAccessToken(accessToken)).thenReturn(false);
		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED)).when(authService).logout(authHeader);
		
		mockMvc.perform(post("/api/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, authHeader)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testRefreshToken_positive() throws Exception {
		String refreshToken = "mockRefreshToken";
		RefreshTokenRequest dto = new RefreshTokenRequest(refreshToken);
		
		JwtResponse jwtResponse = JwtResponse.builder()
				.accessToken("mockAccessToken")
				.refreshToken("mockRefreshToken")
				.build();
		
		when(authService.refreshToken(dto.getRefreshToken())).thenReturn(jwtResponse);
		
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(dto)))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.accessToken").value("mockAccessToken"));
	}
	
	@Test
	public void testRefreshToken_negative_blacklistedRefreshToken() throws Exception {
		String refreshToken = "blacklistedMockRefreshToken";
		RefreshTokenRequest dto = new RefreshTokenRequest(refreshToken);
		
		when(authService.refreshToken(dto.getRefreshToken())).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(dto)))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testRefreshToken_negative_tokenNotFound() throws Exception {
		String refreshToken = "notFoundRefreshToken";
		RefreshTokenRequest dto = new RefreshTokenRequest(refreshToken);
		
		when(authService.refreshToken(dto.getRefreshToken()))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(dto)))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testRefreshToken_negative_tokenExpired() throws Exception {
		String refreshToken = "expiredMockRefreshToken";
		RefreshTokenRequest dto = new RefreshTokenRequest(refreshToken);
		
		when(authService.refreshToken(dto.getRefreshToken()))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(dto)))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testRefreshToken_negative_tokenRevoked() throws Exception {
		String refreshToken = "revokedMockRefreshToken";
		RefreshTokenRequest dto = new RefreshTokenRequest(refreshToken);
		
		when(authService.refreshToken(dto.getRefreshToken()))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(dto)))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testRefreshToken_negative_emptyToken() throws Exception {
		RefreshTokenRequest dto = new RefreshTokenRequest("");
		
		when(jwtService.verifyRefreshToken(dto.getRefreshToken()))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		
		when(authService.refreshToken(dto.getRefreshToken())).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(dto)))
				.andExpect(status().isUnauthorized());
	}
	
	@Test
	public void testRefreshToken_negative_invalidRefreshToken() throws Exception {
		String refreshToken = "invalidMockRefreshToken";
		RefreshTokenRequest dto = new RefreshTokenRequest(refreshToken);
		
		when(authService.refreshToken(dto.getRefreshToken())).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON)
						.content(new ObjectMapper().writeValueAsString(dto)))
				.andExpect(status().isUnauthorized());
	}
}
