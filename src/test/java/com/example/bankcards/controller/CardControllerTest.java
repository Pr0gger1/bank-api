package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.BankCardStatus;
import com.example.bankcards.enums.Role;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.AuthService;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.security.TokenBlacklistService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardController.class)
@EnableMethodSecurity
public class CardControllerTest {
	
	private final String token = "Bearer someToken";
	
	@Autowired
	private MockMvc mockMvc;
	
	@MockitoBean
	private TokenBlacklistService blacklistService;
	
	@MockitoBean
	private AuthService authService;
	
	@MockitoBean
	private UserRepository userRepository;
	
	@MockitoBean
	private JwtService jwtService;
	
	@MockitoBean
	private UserService userService;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@MockitoBean
	private CardService cardService;
	private UUID testCardId;
	private UUID nonExistentCardId;
	private CardDto testCardDto;
	private User testUser;
	
	@BeforeEach
	void setUp() {
		testCardId = UUID.randomUUID();
		nonExistentCardId = UUID.randomUUID();
		testUser = User.builder()
				.id(UUID.randomUUID())
				.email("testuser@example.com")
				.role(Role.USER)
				.build();
		
		testCardDto = CardDto.builder()
				.id(testCardId)
				.maskedNumber("************1234")
				.owner(testUser)
				.status(BankCardStatus.ACTIVE)
				.expiryDate(LocalDate.now().plusYears(3))
				.build();
	}
	
	@Test
	@WithMockUser(authorities = "ADMIN")
	void deleteCard_ShouldReturnOk_WhenAdmin() throws Exception {
		doNothing().when(cardService).deleteCard(testCardId);
		
		mockMvc.perform(delete("/api/cards/delete/{id}", testCardId)
						.header(HttpHeaders.AUTHORIZATION, token)
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(content().string("Bank card deleted successfully"));
		
		verify(cardService, times(1)).deleteCard(testCardId);
	}
	
	@Test
	@WithMockUser(authorities = "ADMIN")
	void blockCard_ShouldReturnOk_WhenAdmin() throws Exception {
		doNothing().when(cardService).blockCard(testCardId);
		
		mockMvc.perform(patch("/api/cards/block/{id}", testCardId)
						.header(HttpHeaders.AUTHORIZATION, token)
						.with(csrf()))
				.andExpect(status().isOk());
		
		verify(cardService, times(1)).blockCard(testCardId);
	}
	
	@Test
	@WithMockUser(authorities = "ADMIN")
	void activateCard_ShouldReturnOk_WhenAdmin() throws Exception {
		doNothing().when(cardService).activateCard(testCardId);
		
		mockMvc.perform(patch("/api/cards/activate/{id}", testCardId)
						.header(HttpHeaders.AUTHORIZATION, token)
						.with(csrf()))
				.andExpect(status().isOk());
		
		verify(cardService, times(1)).activateCard(testCardId);
	}
	
	@Test
	@WithMockUser(authorities = "ADMIN")
	void createCard_ShouldReturnCreatedCard_WhenValidRequest() throws Exception {
		CreateCardRequest request = CreateCardRequest.builder()
				.periodInYears(5)
				.userId(testUser.getId())
				.build();
		
		when(cardService.createCard(any(CreateCardRequest.class))).thenReturn(testCardDto);
		
		mockMvc.perform(post("/api/cards/create")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, token)
						.content(objectMapper.writeValueAsString(request))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(testCardDto.getId().toString()))
				.andExpect(jsonPath("$.maskedNumber").value(testCardDto.getMaskedNumber()));
		
		verify(cardService, times(1)).createCard(any(CreateCardRequest.class));
	}
	
	@Test
	@WithMockUser
	void getCards_ShouldReturnPageOfCards() throws Exception {
		Page<CardDto> page = new PageImpl<>(List.of(testCardDto));
		
		when(cardService.getCards(eq(1), eq(10))).thenReturn(page);
		
		mockMvc.perform(get("/api/cards")
						.header(HttpHeaders.AUTHORIZATION, token)
						.param("page", "1")
						.param("size", "11"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(testCardDto.getId().toString()))
				.andExpect(jsonPath("$.content[0].maskedNumber").value(testCardDto.getMaskedNumber()));
		
		verify(cardService, times(1)).getCards(1, 10);
	}
	
	@Test
	@WithMockUser(authorities = "USER")
	void deleteCard_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
		mockMvc.perform(delete("/api/cards/delete/{id}", testCardId)
						.header(HttpHeaders.AUTHORIZATION, token)
						.with(csrf()))
				.andExpect(status().isForbidden());
		
		verify(cardService, never()).deleteCard(any());
	}
	
	@Test
	@WithMockUser(authorities = "USER")
	void blockCard_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
		mockMvc.perform(patch("/api/cards/block/{id}", testCardId)
						.header(HttpHeaders.AUTHORIZATION, token)
						.with(csrf()))
				.andExpect(status().isForbidden());
		
		verify(cardService, never()).blockCard(any());
	}
	
	@Test
	@WithMockUser(authorities = "USER")
	void activateCard_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
		mockMvc.perform(patch("/api/cards/activate/{id}", testCardId)
						.header(HttpHeaders.AUTHORIZATION, token)
						.with(csrf()))
				.andExpect(status().isForbidden());
		
		verify(cardService, never()).activateCard(any());
	}
	
	@Test
	@WithMockUser(authorities = "USER")
	void createCard_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
		CreateCardRequest request = CreateCardRequest.builder()
				.periodInYears(5)
				.userId(testUser.getId())
				.build();
		
		mockMvc.perform(post("/api/cards/create")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, token)
						.content(objectMapper.writeValueAsString(request))
						.with(csrf()))
				.andExpect(status().isForbidden());
		
		verify(cardService, never()).createCard(any());
	}
	
	@Test
	void deleteCard_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
		mockMvc.perform(delete("/api/cards/delete/{id}", testCardId)
						.with(csrf()))
				.andExpect(status().isUnauthorized());
		
		verify(cardService, never()).deleteCard(any());
	}
	
	// ========== NEGATIVE TESTS - VALIDATION ==========
	
	@Test
	@WithMockUser(authorities = "ADMIN")
	void createCard_ShouldReturnBadRequest_WhenUserIdIsNull() throws Exception {
		CreateCardRequest request = CreateCardRequest.builder()
				.userId(null)
				.periodInYears(5)
				.build();
		
		mockMvc.perform(post("/api/cards/create")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, token)
						.content(objectMapper.writeValueAsString(request))
						.with(csrf()))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	@WithMockUser(authorities = "ADMIN")
	void createCard_ShouldReturnBadRequest_WhenPeriodInYearsIsZero() throws Exception {
		CreateCardRequest request = CreateCardRequest.builder()
				.userId(testUser.getId())
				.periodInYears(0)
				.build();
		
		mockMvc.perform(post("/api/cards/create")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, token)
						.content(objectMapper.writeValueAsString(request))
						.with(csrf()))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	@WithMockUser(authorities = "ADMIN")
	void createCard_ShouldReturnBadRequest_WhenPeriodInYearsIsNegative() throws Exception {
		CreateCardRequest request = CreateCardRequest.builder()
				.userId(testUser.getId())
				.periodInYears(-1)
				.build();
		
		mockMvc.perform(post("/api/cards/create")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, token)
						.content(objectMapper.writeValueAsString(request))
						.with(csrf()))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	@WithMockUser(authorities = "ADMIN")
	void createCard_ShouldReturnBadRequest_WhenRequestBodyIsInvalid() throws Exception {
		String invalidJson = "{ invalid json }";
		
		mockMvc.perform(post("/api/cards/create")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, token)
						.content(invalidJson)
						.with(csrf()))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	@WithMockUser(authorities = "ADMIN")
	void createCard_ShouldReturnBadRequest_WhenContentTypeIsWrong() throws Exception {
		CreateCardRequest request = CreateCardRequest.builder()
				.userId(testUser.getId())
				.periodInYears(5)
				.build();
		
		mockMvc.perform(post("/api/cards/create")
						.contentType(MediaType.TEXT_PLAIN)
						.header(HttpHeaders.AUTHORIZATION, token)
						.content(objectMapper.writeValueAsString(request))
						.with(csrf()))
				.andExpect(status().isUnsupportedMediaType());
	}
	
	@Test
	@WithMockUser(authorities = "ADMIN")
	void deleteCard_ShouldReturnNotFound_WhenCardDoesNotExist() throws Exception {
		doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"))
				.when(cardService).deleteCard(nonExistentCardId);
		
		mockMvc.perform(delete("/api/cards/delete/{id}", nonExistentCardId)
						.header(HttpHeaders.AUTHORIZATION, token)
						.with(csrf()))
				.andExpect(status().isNotFound());
	}
	
	@Test
	@WithMockUser(authorities = "ADMIN")
	void blockCard_ShouldReturnNotFound_WhenCardDoesNotExist() throws Exception {
		doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"))
				.when(cardService).blockCard(nonExistentCardId);
		
		mockMvc.perform(patch("/api/cards/block/{id}", nonExistentCardId)
						.header(HttpHeaders.AUTHORIZATION, token)
						.with(csrf()))
				.andExpect(status().isNotFound());
	}
	
	@Test
	@WithMockUser
	void getCards_ShouldReturnBadRequest_WhenPageIsNegative() throws Exception {
		mockMvc.perform(get("/api/cards")
						.header(HttpHeaders.AUTHORIZATION, token)
						.param("page", "-1")
						.param("size", "10"))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	@WithMockUser
	void getCards_ShouldReturnBadRequest_WhenSizeIsZero() throws Exception {
		mockMvc.perform(get("/api/cards")
						.header(HttpHeaders.AUTHORIZATION, token)
						.param("page", "1")
						.param("size", "0"))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	@WithMockUser
	void getCards_ShouldReturnBadRequest_WhenSizeIsTooLarge() throws Exception {
		mockMvc.perform(get("/api/cards")
						.header(HttpHeaders.AUTHORIZATION, token)
						.param("page", "1")
						.param("size", "1000"))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	@WithMockUser
	void getCards_ShouldReturnEmptyPage_WhenNoCardsExist() throws Exception {
		Page<CardDto> emptyPage = new PageImpl<>(Collections.emptyList());
		when(cardService.getCards(anyInt(), anyInt())).thenReturn(emptyPage);
		
		mockMvc.perform(get("/api/cards")
						.header(HttpHeaders.AUTHORIZATION, token)
						.param("page", "1")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty());
	}
	
	// ========== NEGATIVE TESTS - CSRF PROTECTION ==========
	
	@Test
	@WithMockUser(authorities = "ADMIN")
	void deleteCard_ShouldReturnForbidden_WhenCsrfTokenIsMissing() throws Exception {
		mockMvc.perform(delete("/api/cards/delete/{id}", testCardId)
						.header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isForbidden());
		
		verify(cardService, never()).deleteCard(any());
	}
	
	@Test
	@WithMockUser(authorities = "ADMIN")
	void createCard_ShouldReturnForbidden_WhenCsrfTokenIsMissing() throws Exception {
		CreateCardRequest request = CreateCardRequest.builder()
				.userId(testUser.getId())
				.periodInYears(5)
				.build();
		
		mockMvc.perform(post("/api/cards/create")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, token)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden());
		
		verify(cardService, never()).createCard(any());
	}
	
	@Test
	@WithMockUser
	void getCards_ShouldReturnMethodNotAllowed_WhenUsingPost() throws Exception {
		mockMvc.perform(post("/api/cards")
						.header(HttpHeaders.AUTHORIZATION, token)
						.with(csrf()))
				.andExpect(status().isMethodNotAllowed());
	}
}