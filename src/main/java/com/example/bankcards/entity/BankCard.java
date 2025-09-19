package com.example.bankcards.entity;

import com.example.bankcards.enums.BankCardStatus;
import com.example.bankcards.util.SixteenDigitId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "cards")
@Entity
public class BankCard {
	@Id
	@SixteenDigitId
	@Size(min = 16, max = 16, message = "Card number must be 16 digits")
	@Pattern(regexp = "^\\d{16}$", message = "Card number must be 16 digits")
	private String number;
	
	@PositiveOrZero(message = "Balance must be positive or zero")
	private float balance;
	
	private BankCardStatus status;
	
	@JsonSerialize(using = JsonSerializer.class)
	private LocalDate validityPeriod;
	
	public String getNumber() {
		String lastNumbers = number.substring(number.length() - 4);
		String group = "*".repeat(4);
		
		StringBuilder builder = new StringBuilder();
		
		for (int i = 0; i < 2; i++) {
			builder.append(group);
			builder.append(" ");
		}
		
		builder.append(lastNumbers);
		
		return builder.toString();
	}
}
