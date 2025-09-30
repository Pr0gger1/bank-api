package com.example.bankcards.dto;

import com.example.bankcards.entity.Card;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CardDtoWithBalance extends CardDto {
	private BigDecimal balance;
	
	public CardDtoWithBalance(Card card) {
		super(
			card.getId(),
			card.getMaskedNumber(),
			card.getOwner(),
			card.getStatus(),
			card.getExpiryDate()
		);
		this.balance = card.getBalance();
	}
}
