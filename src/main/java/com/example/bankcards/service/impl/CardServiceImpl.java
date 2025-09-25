package com.example.bankcards.service.impl;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.BankCardStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.CardUtils;
import com.example.bankcards.util.mappers.CardMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
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
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found");
		}
		
		cardRepository.deleteById(id);
	}
	
	@Override
	@Transactional
	public CardDto createCard(CreateCardRequest createCardRequest) {
		Optional<User> userResponse = userRepository.findById(createCardRequest.getUserId());
		
		if (userResponse.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}
		
		Card card = Card.builder()
				.owner(userResponse.get())
				.expiryDate(LocalDate.now().plusYears(createCardRequest.getPeriodInYears()))
				.number(cardUtils.generateNumber())
				.build();
		
		cardRepository.save(card);
		
		return cardMapper.cardToCardDto(card);
	}
	
	@Override
	public Page<CardDto> getCards(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		
		return cardRepository.findAll(pageable).map(cardMapper::cardToCardDto);
	}
	
	@Override
	public void blockCard(UUID id) {
		changeCardStatus(id, BankCardStatus.BLOCKED);
	}
	
	@Override
	public void activateCard(UUID id) {
		changeCardStatus(id, BankCardStatus.ACTIVE);
	}
	
	private void changeCardStatus(UUID id, BankCardStatus status) {
		Optional<Card> cardResponse = cardRepository.findById(id);
		
		if (cardResponse.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found");
		}
		
		Card card = cardResponse.get();
		card.setStatus(status);
		
		cardRepository.save(card);
	}
}
