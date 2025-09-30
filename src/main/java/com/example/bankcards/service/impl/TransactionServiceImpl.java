package com.example.bankcards.service.impl;

import com.example.bankcards.dto.request.TransactionRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.BankCardStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.util.Constants;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
	private final CardRepository cardRepository;
	
	@Override
	@Transactional
	public void makeTransactionBetweenCards(
			TransactionRequest request,
			User user
	) {
		UUID senderCardId = request.getSender_card_id();
		UUID recipientCardId = request.getRecipient_card_id();
		BigDecimal amount = request.getAmount();
		
		if (senderCardId.equals(recipientCardId)) {
			log.error("makeTransactionBetweenCards[1]: Sender and recipient cards are the same");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sender and recipient cards must be different");
		}
		
		log.info(
				"makeTransactionBetweenCards[2]: Starting transaction (senderCardId={}, recipientCardId={}, amount={})",
				senderCardId, recipientCardId, amount
		);
		
		Card senderCard = cardRepository.findByOwnerAndId(user, senderCardId)
				.orElseThrow(() -> {
					log.error("makeTransactionBetweenCards[3]: Sender card not found ({})", senderCardId);
					return new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender card not found");
				});
		
		Card recipientCard = cardRepository.findById(recipientCardId)
				.orElseThrow(() -> {
					log.error("makeTransactionBetweenCards[4]: Recipient card not found ({})", recipientCardId);
					return new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient card not found");
				});
		
		log.debug(
			"makeTransactionBetweenCards[5]: Sender card before transaction (id={}, balance={}, status={})",
			senderCard.getId(), senderCard.getBalance(), senderCard.getStatus()
		);
		
		log.debug(
			"makeTransactionBetweenCards[6]: Recipient card before transaction (id={}, balance={}, status={})",
			recipientCard.getId(), recipientCard.getBalance(), recipientCard.getStatus()
		);
		
		validateTransaction(amount, senderCard, recipientCard);
		
		BigDecimal senderCardBalance = senderCard.getBalance().subtract(amount);
		BigDecimal recipientCardBalance = recipientCard.getBalance().add(amount);
		
		senderCard.setBalance(senderCardBalance);
		recipientCard.setBalance(recipientCardBalance);
		
		cardRepository.save(senderCard);
		cardRepository.save(recipientCard);
		
		log.info(
			"makeTransactionBetweenCards[7]: Transaction completed successfully (newSenderBalance={}, newRecipientBalance={})",
			senderCard.getBalance(),
			recipientCard.getBalance()
		);
	}
	
	private void validateTransaction(BigDecimal amount, Card senderCard, Card recipientCard) {
		log.debug(
			"validateTransaction[1]: Validating transaction (amount={}, senderId={}, recipientId={})",
			amount, senderCard.getId(), recipientCard.getId()
		);
		
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			log.warn("validateTransaction[2]: Invalid transaction amount: {}", amount);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
		}
		
		if (senderCard.getStatus() != BankCardStatus.ACTIVE) {
			log.warn("validateTransaction[3]: Sender card is not active: id={}, status={}", senderCard.getId(), senderCard.getStatus());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sender card is not active");
		}
		
		if (recipientCard.getStatus() != BankCardStatus.ACTIVE) {
			log.warn("validateTransaction[4]: Recipient card is not active: id={}, status={}", recipientCard.getId(), recipientCard.getStatus());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient card is not active");
		}
		
		if (senderCard.getBalance().compareTo(amount) < 0) {
			log.warn("validateTransaction[5]: Not enough funds: senderId={}, balance={}, attemptedAmount={}",
					senderCard.getId(), senderCard.getBalance(), amount);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.NOT_ENOUGH_MONEY_ERROR_MESSAGE);
		}
		
		log.debug(
			"validateTransaction[7]: Transaction validation passed for senderId={}, recipientId={}",
			senderCard.getId(), recipientCard.getId()
		);
	}
}
