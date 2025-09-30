package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.response.CardBalanceResponse;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface CardService {
	void deleteCard(UUID id);
	CardDto getCardById(User user, UUID id);
	CardDto createCard(CreateCardRequest createCardRequest);
	Page<CardDto> getCards(int page, int size, String search, User user);
	void blockCard(UUID id);
	void activateCard(UUID id);
	
	CardBalanceResponse getCardBalance(User user, UUID id);
}
