package com.example.bankcards.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
	@NotNull(message = "Sender card id must not be null")
	private UUID sender_card_id;
	
	@NotNull(message = "Recipient card id must not be null")
	private UUID recipient_card_id;
	
	@Positive(message = "Amount must be positive")
	@NotNull(message = "Amount must not be null")
	private BigDecimal amount;
}
