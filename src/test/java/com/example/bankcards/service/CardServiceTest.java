package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.response.CardBalanceResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.BankCardStatus;
import com.example.bankcards.enums.Role;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.impl.CardServiceImpl;
import com.example.bankcards.util.CardUtils;
import com.example.bankcards.util.mappers.CardMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {
	
	@Mock private CardRepository cardRepository;
	@Mock private UserRepository userRepository;
	@Mock private CardMapper cardMapper;
	@Mock private CardUtils cardUtils;
	
	@InjectMocks
	private CardServiceImpl cardService;
	
	private User user;
	private Card card;
	private UUID cardId;
	
	@BeforeEach
	void setUp() {
		user = User.builder()
				.id(UUID.randomUUID())
				.firstName("John")
				.lastName("Doe")
				.role(Role.USER)
				.build();
		
		cardId = UUID.randomUUID();
		card = Card.builder()
				.id(cardId)
				.owner(user)
				.balance(BigDecimal.valueOf(1000))
				.status(BankCardStatus.ACTIVE)
				.number("1234567890123456")
				.build();
	}
	
	@Test
	void deleteCard_ShouldDelete_WhenCardExists() {
		when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
		
		cardService.deleteCard(cardId);
		
		verify(cardRepository).deleteById(cardId);
	}
	
	@Test
	void deleteCard_ShouldThrow_WhenCardNotFound() {
		when(cardRepository.findById(cardId)).thenReturn(Optional.empty());
		
		assertThrows(ResponseStatusException.class,
				() -> cardService.deleteCard(cardId));
	}
	
	@Test
	void getCardById_ShouldReturnDto_WhenUserIsOwner() {
		CardDto dto = new CardDto();
		when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
		when(cardMapper.cardToCardDto(card)).thenReturn(dto);
		
		CardDto result = cardService.getCardById(user, cardId);
		
		assertEquals(dto, result);
	}
	
	@Test
	void getCardById_ShouldReturnDto_WhenUserIsAdmin() {
		User admin = User.builder()
				.id(UUID.randomUUID())
				.role(Role.ADMIN)
				.build();
		
		CardDto dto = new CardDto();
		when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
		when(cardMapper.cardToCardDto(card)).thenReturn(dto);
		
		CardDto result = cardService.getCardById(admin, cardId);
		
		assertEquals(dto, result);
	}
	
	@Test
	void getCardById_ShouldThrowForbidden_WhenNotOwnerAndNotAdmin() {
		User anotherUser = User.builder()
				.id(UUID.randomUUID())
				.role(Role.USER)
				.build();
		
		when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
		
		assertThrows(ResponseStatusException.class,
				() -> cardService.getCardById(anotherUser, cardId));
	}
	
	@Test
	void getCardById_ShouldThrowNotFound_WhenCardMissing() {
		when(cardRepository.findById(cardId)).thenReturn(Optional.empty());
		
		assertThrows(ResponseStatusException.class,
				() -> cardService.getCardById(user, cardId));
	}
	
	@Test
	void createCard_ShouldSaveAndReturnDto_WhenUserExists() {
		CreateCardRequest request = CreateCardRequest.builder()
				.user_id(user.getId())
				.period(3)
				.build();
		
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(cardUtils.generateNumber()).thenReturn("9999888877776666");
		when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
		when(cardMapper.cardToCardDto(any(Card.class))).thenReturn(new CardDto());
		
		CardDto result = cardService.createCard(request);
		
		assertNotNull(result);
		verify(cardRepository).save(any(Card.class));
	}
	
	@Test
	void createCard_ShouldThrow_WhenUserNotFound() {
		CreateCardRequest request = CreateCardRequest.builder()
				.user_id(UUID.randomUUID())
				.period(2)
				.build();
		
		when(userRepository.findById(request.getUser_id())).thenReturn(Optional.empty());
		
		assertThrows(ResponseStatusException.class,
				() -> cardService.createCard(request));
	}
	
	@Test
	void getCards_ShouldReturnAll_WhenNoSearchTermAndAdmin() {
		Page<Card> page = new PageImpl<>(List.of(card));
		User adminUser = User.builder()
				.id(UUID.randomUUID())
				.role(Role.ADMIN)
				.build();
		
		when(cardRepository.findAll(any(Pageable.class))).thenReturn(page);
		
		Page<CardDto> result = cardService.getCards(0, 10, null, adminUser);
		
		assertEquals(1, result.getTotalElements());
	}
	
	@Test
	void getCards_ShouldReturnAll_WhenNoSearchTermAndUser() {
		Page<Card> page = new PageImpl<>(List.of(card));
		when(cardRepository.findByOwner(eq(user), any(Pageable.class))).thenReturn(page);
		
		Page<CardDto> result = cardService.getCards(0, 10, null, user);
		
		assertEquals(1, result.getTotalElements());
	}
	
	@Test
	void getCards_ShouldSearchByLastFourDigits_WhenDigitsProvided() {
		Page<Card> page = new PageImpl<>(List.of(card));
		when(cardRepository.findByLastFourDigits(eq("1234"), any(Pageable.class)))
				.thenReturn(page);
		when(cardMapper.cardToCardDto(card)).thenReturn(new CardDto());
		
		Page<CardDto> result = cardService.getCards(0, 10, "1234", user);
		
		assertEquals(1, result.getTotalElements());
	}
	
	@Test
	void getCards_ShouldSearchByFullName_WhenTwoWordsProvided() {
		Page<Card> page = new PageImpl<>(List.of(card));
		when(cardRepository.findByOwnerFirstNameOrLastName(eq("John"), eq("Doe"), any(Pageable.class)))
				.thenReturn(page);
		when(cardMapper.cardToCardDto(card)).thenReturn(new CardDto());
		
		Page<CardDto> result = cardService.getCards(0, 10, "John Doe", user);
		
		assertEquals(1, result.getTotalElements());
	}
	
	@Test
	void getCards_ShouldSearchByFirstName_WhenOneWordProvided() {
		Page<Card> page = new PageImpl<>(List.of(card));
		when(cardRepository.findByOwnerFirstName(eq("John"), any(Pageable.class)))
				.thenReturn(page);
		when(cardMapper.cardToCardDto(card)).thenReturn(new CardDto());
		
		Page<CardDto> result = cardService.getCards(0, 10, "John", user);
		
		assertEquals(1, result.getTotalElements());
	}
	
	@Test
	void blockCard_ShouldChangeStatus_WhenCardExists() {
		when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
		
		cardService.blockCard(cardId);
		
		assertEquals(BankCardStatus.BLOCKED, card.getStatus());
		verify(cardRepository).save(card);
	}
	
	@Test
	void blockCard_ShouldThrow_WhenCardNotFound() {
		when(cardRepository.findById(cardId)).thenReturn(Optional.empty());
		
		assertThrows(ResponseStatusException.class,
				() -> cardService.blockCard(cardId));
	}
	
	@Test
	void activateCard_ShouldChangeStatus_WhenCardExists() {
		when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
		
		cardService.activateCard(cardId);
		
		assertEquals(BankCardStatus.ACTIVE, card.getStatus());
		verify(cardRepository).save(card);
	}
	
	@Test
	void getCardBalance_ShouldReturnBalance_WhenCardExists() {
		when(cardRepository.findByOwnerAndId(user, cardId)).thenReturn(Optional.of(card));
		
		CardBalanceResponse response = cardService.getCardBalance(user, cardId);
		
		assertEquals(card.getBalance(), response.getBalance());
		assertTrue(response.getMaskedNumber().contains("****"));
	}
	
	@Test
	void getCardBalance_ShouldThrow_WhenCardNotFound() {
		when(cardRepository.findByOwnerAndId(user, cardId)).thenReturn(Optional.empty());
		
		assertThrows(ResponseStatusException.class,
				() -> cardService.getCardBalance(user, cardId));
	}
}
