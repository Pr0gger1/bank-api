package com.example.bankcards.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardBalanceResponse implements Serializable {
	private BigDecimal balance;
	
	@JsonProperty("cardNumber")
	private String maskedNumber;
}
