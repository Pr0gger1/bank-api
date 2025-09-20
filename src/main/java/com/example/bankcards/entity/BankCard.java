package com.example.bankcards.entity;

import com.example.bankcards.enums.BankCardStatus;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "cards")
@Entity
public class BankCard {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(nullable = false, unique = true)
	private String number;
	
	@PositiveOrZero(message = "Balance must be positive or zero")
	@Column(scale = 2, precision = 19)
	private BigDecimal balance;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User owner;
	
	@Enumerated(EnumType.STRING)
	private BankCardStatus status = BankCardStatus.ACTIVE;
	
	@JsonSerialize(using = JsonSerializer.class)
	private LocalDate expiryDate;
	
	public String getMaskedNumber() {
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
