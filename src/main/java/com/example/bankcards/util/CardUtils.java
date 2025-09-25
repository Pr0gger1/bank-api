package com.example.bankcards.util;

import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardUtils {
	public static int NUMBER_LENGTH = 16;
	private final CardRepository cardRepository;
	
	public String generateNumber() {
		StringBuilder sb = new StringBuilder();
		int firstNumber = (int) (Math.random() * 10);
		
		sb.append(firstNumber);
		
		for (int i = 0; i < NUMBER_LENGTH - 1; i++) {
			sb.append((int) (Math.random() * 10));
		}
		
		String number = sb.toString();
		
		if (cardRepository.existsByNumber(number)) {
			return generateNumber();
		}
		
		return number;
	}
}
