package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.response.CardBalanceResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {
	private final CardService cardService;
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<String> deleteCard(@PathVariable UUID id) {
		cardService.deleteCard(id);
		
		return ResponseEntity.ok("Bank card deleted successfully");
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<CardDto> getCardById(
			@AuthenticationPrincipal User user,
			@PathVariable UUID id
	) {
		return ResponseEntity.ok(cardService.getCardById(user, id));
	}
	
	@PatchMapping("/block/{id}")
	public ResponseEntity<String> blockCard(@PathVariable UUID id) {
		cardService.blockCard(id);
		
		return ResponseEntity.ok("Bank card blocked successfully");
	}
	
	@GetMapping("/balance/{id}")
	public ResponseEntity<CardBalanceResponse> getCardBalance(
			@AuthenticationPrincipal User user,
			@PathVariable UUID id
	) {
		return ResponseEntity.ok(cardService.getCardBalance(user, id));
	}
	
	@PatchMapping("/activate/{id}")
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<String> activateCard(@PathVariable UUID id) {
		cardService.activateCard(id);
		
		return ResponseEntity.ok("Bank card activated successfully");
	}
	
	@PostMapping
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<CardDto> createCard(@RequestBody @Valid CreateCardRequest createCardRequest) {
		return ResponseEntity.ok(cardService.createCard(createCardRequest));
	}
	
	@GetMapping
	public ResponseEntity<Page<CardDto>> getCards(
			@RequestParam(required = false, defaultValue = "1")
			@Min(1)
			int page,
			
			@RequestParam(required = false, defaultValue = "10")
			@Min(1) @Max(100)
			int size,
			@RequestParam(required = false)
			String q,
			@AuthenticationPrincipal User user
	) {
		return ResponseEntity.ok(cardService.getCards(page - 1, size, q, user));
	}
}
