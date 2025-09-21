package com.example.bankcards.service.impl;

import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;
	
	@Override
	public User getUserById(UUID id) {
		return null;
	}
	
	@Override
	public User getUserByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(
						() -> new ResponseStatusException(
								HttpStatus.NOT_FOUND,
								"User with email " + email + " not found"
						)
				);
	}
	
	@Override
	public boolean checkUserExistence(String email) {
		return userRepository.existsByEmail(email);
	}
}
