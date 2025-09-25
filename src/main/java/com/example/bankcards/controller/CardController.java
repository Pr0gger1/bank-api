package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {
	private final CardService cardService;
	
	@DeleteMapping("/delete/{id}")
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<String> deleteCard(@PathVariable UUID id) {
		cardService.deleteCard(id);
		
		return ResponseEntity.ok("Bank card deleted successfully");
	}
	
	@PatchMapping("/block/{id}")
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<String> blockCard(@PathVariable UUID id) {
		cardService.blockCard(id);
		
		return ResponseEntity.ok("Bank card blocked successfully");
	}
	
	@PatchMapping("/activate/{id}")
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<String> activateCard(@PathVariable UUID id) {
		cardService.activateCard(id);
		
		return ResponseEntity.ok("Bank card activated successfully");
	}
	
	@PostMapping("/create")
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
			int size
	) {
		return ResponseEntity.ok(cardService.getCards(page, size - 1));
	}
}
