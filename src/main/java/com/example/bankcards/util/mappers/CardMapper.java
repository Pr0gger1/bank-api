package com.example.bankcards.util.mappers;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CardMapper {
	@Mapping(target = "maskedNumber", expression = "java(card.getMaskedNumber())")
	CardDto cardToCardDto(Card card);
	Card cardDtoToCard(CardDto card);
}
