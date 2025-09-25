package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.request.CreateCardRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface CardService {
	void deleteCard(UUID id);
	CardDto createCard(CreateCardRequest createCardRequest);
	Page<CardDto> getCards(int page, int size);
	void blockCard(UUID id);
	void activateCard(UUID id);
}
