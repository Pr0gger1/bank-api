package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransactionRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER')")
public class TransactionController {
	private final TransactionService transactionService;
	
	@PostMapping
	public ResponseEntity<String> makeTransaction(
		@RequestBody @Valid TransactionRequest request,
		@AuthenticationPrincipal User user
	) {
		transactionService.makeTransactionBetweenCards(request, user);
		
		return ResponseEntity.ok(Constants.TRANSACTION_MAKE_SUCCESS_MESSAGE);
	}
}
