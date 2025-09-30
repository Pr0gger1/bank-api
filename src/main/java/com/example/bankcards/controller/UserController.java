package com.example.bankcards.controller;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.service.UserService;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class UserController {
	private final UserService userService;
	
	@GetMapping
	public ResponseEntity<Page<UserDto>> getAllUsers(
			@RequestParam(required = false, defaultValue = "1")
			@Min(1)
			int page,
			@RequestParam(required = false, defaultValue = "10")
			@Min(1) @Max(100)
			int size,
			@RequestParam(required = false)
			String q
	) {
		return ResponseEntity.ok(userService.getUsers(page - 1, size, q));
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
		return ResponseEntity.ok(userService.getUserById(id));
	}
	
	@PutMapping
	public ResponseEntity<UserDto> updateUser(@RequestBody @Valid UserDto user) {
		return ResponseEntity.ok(userService.updateUser(user));
	}
	
	@PostMapping
	public ResponseEntity<UserDto> createUser(@RequestBody RegisterRequest request) {
		return ResponseEntity.ok(userService.createUser(request));
	}
	
	@GetMapping("/current")
	@PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
	public ResponseEntity<UserDto> getCurrentUser() {
		return ResponseEntity.ok(userService.getCurrentUser());
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
		userService.deleteUser(id);
		
		return ResponseEntity.noContent().build();
	}
}
