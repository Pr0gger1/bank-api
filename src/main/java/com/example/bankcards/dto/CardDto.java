package com.example.bankcards.dto;

import com.example.bankcards.entity.User;
import com.example.bankcards.enums.BankCardStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDto {
	private UUID id;
	private String maskedNumber;
	private User owner;
	private BankCardStatus status;
	private LocalDate expiryDate;
}
