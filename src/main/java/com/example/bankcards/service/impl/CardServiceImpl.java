package com.example.bankcards.service.impl;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardDtoWithBalance;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.response.CardBalanceResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.BankCardStatus;
import com.example.bankcards.enums.Role;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.CardUtils;
import com.example.bankcards.util.Constants;
import com.example.bankcards.util.UserUtils;
import com.example.bankcards.util.mappers.CardMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
	private final CardRepository cardRepository;
	private final UserRepository userRepository;
	private final CardMapper cardMapper;
	private final CardUtils cardUtils;
	
	@Override
	@Transactional
	public void deleteCard(UUID id) {
		if (cardRepository.findById(id).isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND,
					Constants.CARD_NOT_FOUND_ERROR_MESSAGE
			);
		}
		
		cardRepository.deleteById(id);
	}
	
	@Override
	public CardDto getCardById(User user, UUID id) {
		Optional<Card> cardResponse = cardRepository.findById(id);
		
		if (cardResponse.isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND,
					Constants.CARD_NOT_FOUND_ERROR_MESSAGE
			);
		}
		
		log.info("getCardById[1]: user authorities are {}", user.getAuthorities());
		
		boolean isAdmin = UserUtils.hasRole(user, Role.ADMIN);
		Card card = cardResponse.get();
		
		if (!(isAdmin || card.getOwner().equals(user))) {
			throw new ResponseStatusException(
					HttpStatus.FORBIDDEN,
					Constants.GET_CARD_FORBIDDEN_ERROR_MESSAGE
			);
		}
		
		return cardMapper.cardToCardDto(card);
	}
	
	@Override
	@Transactional
	public CardDto createCard(CreateCardRequest createCardRequest) {
		Optional<User> userResponse = userRepository.findById(createCardRequest.getUser_id());
		
		if (userResponse.isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND,
					Constants.USER_NOT_FOUND_ERROR_MESSAGE
			);
		}
		
		Card card = Card.builder()
				.owner(userResponse.get())
				.expiryDate(LocalDate.now().plusYears(createCardRequest.getPeriod()))
				.number(cardUtils.generateNumber())
				.build();
		
		cardRepository.save(card);
		
		return cardMapper.cardToCardDto(card);
	}
	
	@Override
	public Page<CardDto> getCards(int page, int size, String search, User user) {
		Pageable pageable = PageRequest.of(page, size);
		
		if (search == null || search.isEmpty()) {
			if (UserUtils.hasRole(user, Role.USER)) {
				return getOwnerCards(user, page, size);
			}
			
			Page<Card> cardPage = cardRepository.findAll(pageable);
			return cardPage.map(cardMapper::cardToCardDto);
		}
		
		return searchCards(page, size, search);
	}
	
	private Page<CardDto> getOwnerCards(User user, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		
		Page<Card> ownerCards = cardRepository.findByOwner(user, pageable);
		return ownerCards.map(CardDtoWithBalance::new);
	}
	
	private Page<CardDto> searchCards(int page, int size, String query) {
		String searchTerm = query.trim();
		Pageable pageable = PageRequest.of(page, size);
		
		if (searchTerm.matches("\\d{4}")) {
			return cardRepository.findByLastFourDigits(searchTerm, pageable)
					.map(cardMapper::cardToCardDto);
		}
		
		List<String> userNames = Arrays.stream(searchTerm.split(" ")).toList();
		
		if (userNames.size() > 1) {
			return cardRepository.findByOwnerFirstNameOrLastName(
							userNames.get(0),
							userNames.get(1),
							pageable
					)
					.map(cardMapper::cardToCardDto);
		}
		
		return cardRepository.findByOwnerFirstName(searchTerm, pageable)
				.map(cardMapper::cardToCardDto);
	}
	
	@Override
	@Transactional
	public void blockCard(UUID id) {
		changeCardStatus(id, BankCardStatus.BLOCKED);
	}
	
	@Override
	@Transactional
	public void activateCard(UUID id) {
		changeCardStatus(id, BankCardStatus.ACTIVE);
	}
	
	@Override
	@Transactional
	public CardBalanceResponse getCardBalance(User user, UUID id) {
		Optional<Card> cardResponse = cardRepository.findByOwnerAndId(user, id);
		
		if (cardResponse.isEmpty()) {
			throw new ResponseStatusException(
				HttpStatus.NOT_FOUND, String.format(
					Constants.USER_DOESNT_HAVE_CARD_ERROR_MESSAGE,
					user.getFirstName(),
					user.getLastName(),
					id
				)
			);
		}
		
		Card card = cardResponse.get();
		
		return CardBalanceResponse.builder()
				.maskedNumber(card.getMaskedNumber())
				.balance(card.getBalance())
				.build();
	}
	
	private void changeCardStatus(UUID id, BankCardStatus status) {
		Optional<Card> cardResponse = cardRepository.findById(id);
		
		if (cardResponse.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, Constants.CARD_NOT_FOUND_ERROR_MESSAGE);
		}
		
		Card card = cardResponse.get();
		card.setStatus(status);
		
		cardRepository.save(card);
	}
}
