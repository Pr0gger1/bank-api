package com.example.bankcards.service;

import com.example.bankcards.dto.request.TransactionRequest;
import com.example.bankcards.entity.User;

public interface TransactionService {
	void makeTransactionBetweenCards(
			TransactionRequest request,
			User user
	);
}
