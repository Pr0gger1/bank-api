package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransactionRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.TokenBlacklistService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.service.UserService;
import com.example.bankcards.service.impl.AuthService;
import com.example.bankcards.service.impl.JwtService;
import com.example.bankcards.util.mappers.CardMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest(TransactionController.class)
class TransactionControllerTest extends BaseControllerTest {
	
	@MockitoBean
	private TransactionService transactionService;
	
	@MockitoBean
	private TokenBlacklistService blacklistService;
	
	@MockitoBean
	private UserRepository userRepository;
	
	@MockitoBean
	private UserService userService;
	
	@MockitoBean
	private AuthService authService;
	
	@MockitoBean
	private JwtService jwtService;
	
	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@MockitoBean
	private CardRepository cardRepository;
	
	@MockitoBean
	private CardService cardService;
	
	@MockitoBean
	private CardMapper cardMapper;
	
	@Test
	@WithMockUser(authorities = "USER")
	void makeTransaction_success() throws Exception {
		TransactionRequest request = TransactionRequest.builder()
				.sender_card_id(UUID.randomUUID())
				.recipient_card_id(UUID.randomUUID())
				.amount(BigDecimal.valueOf(100))
				.build();
		
		Mockito.doNothing().when(transactionService)
				.makeTransactionBetweenCards(eq(request), any(User.class));
		
		mockMvc.perform(post("/api/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, token)
				.with(csrf())
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk());
	}
	
	@Test
	@WithMockUser(authorities = "USER")
	void makeTransaction_missingSenderCardId() throws Exception {
		TransactionRequest request = TransactionRequest.builder()
				.recipient_card_id(UUID.randomUUID())
				.amount(BigDecimal.valueOf(50))
				.build();
		
		mockMvc.perform(post("/api/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, token)
				.with(csrf())
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}
	
	@Test
	@WithMockUser(authorities = "USER")
	void makeTransaction_missingRecipientCardId() throws Exception {
		TransactionRequest request = TransactionRequest.builder()
				.recipient_card_id(UUID.randomUUID())
				.amount(BigDecimal.valueOf(50))
				.build();
		
		mockMvc.perform(post("/api/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, token)
						.with(csrf())
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	@WithMockUser(authorities = "USER")
	void makeTransaction_whenMoneyIsNotEnoughOnSenderCard() throws Exception {
		TransactionRequest request = TransactionRequest.builder()
				.sender_card_id(UUID.randomUUID())
				.recipient_card_id(UUID.randomUUID())
				.amount(BigDecimal.valueOf(10000))
				.build();
		
		doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough money on sender card"))
				.when(transactionService)
				.makeTransactionBetweenCards(any(TransactionRequest.class), any(User.class));
		
		mockMvc.perform(post("/api/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, token)
						.with(csrf())
						.with(authentication(userAuth))
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Not enough money on sender card"));

		verify(transactionService, times(1))
				.makeTransactionBetweenCards(any(TransactionRequest.class), any(User.class));
	}
	
	
	@Test
	@WithMockUser(authorities = "USER")
	void makeTransaction_whenSenderCardIsNotActive() throws Exception {
		TransactionRequest request = TransactionRequest.builder()
				.sender_card_id(UUID.randomUUID())
				.recipient_card_id(UUID.randomUUID())
				.amount(BigDecimal.valueOf(100))
				.build();
		
		doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card is not active"))
				.when(transactionService)
				.makeTransactionBetweenCards(any(TransactionRequest.class), any(User.class));
		
		mockMvc.perform(post("/api/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, token)
				.with(csrf())
				.with(authentication(userAuth))
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Card is not active"));
	}
	
	@Test
	@WithMockUser(authorities = "USER")
	void makeTransaction_whenAmountIsNegative() throws Exception {
		TransactionRequest request = TransactionRequest.builder()
				.sender_card_id(UUID.randomUUID())
				.recipient_card_id(UUID.randomUUID())
				.amount(BigDecimal.valueOf(-100))
				.build();
		
		Mockito.doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST))
				.when(transactionService)
				.makeTransactionBetweenCards(eq(request), any(User.class));
		
		mockMvc.perform(post("/api/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, token)
						.with(csrf())
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}
	
	@Test
	@WithMockUser(authorities = "ADMIN")
	void makeTransaction_forbiddenForAdmin() throws Exception {
		TransactionRequest request = TransactionRequest.builder()
				.sender_card_id(UUID.randomUUID())
				.recipient_card_id(UUID.randomUUID())
				.amount(BigDecimal.valueOf(50))
				.build();
		
		mockMvc.perform(post("/api/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden());
	}
}
