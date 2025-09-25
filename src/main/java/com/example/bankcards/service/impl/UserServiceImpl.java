package com.example.bankcards.service.impl;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	
	@Override
	public User getUserById(UUID id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
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
	@Transactional
	public User createUser(RegisterRequest request) {
		String encodedPassword = passwordEncoder.encode(request.getPassword());
		
		User user = User.builder()
				.email(request.getEmail())
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.patronymic(request.getPatronymic())
				.password(encodedPassword)
				.role(Role.USER)
				.build();
		
		return userRepository.save(user);
	}
	
	@Override
	@Transactional
	public UserDto updateUser(UserDto userDto) {
//		if (userRepository.existsById(userDto.getI))
		return null;
	}
	
	@Override
	public Page<User> getUsers(int page, int size) {
		return null;
	}
	
	@Override
	public User getCurrentUser() {
		String email = SecurityContextHolder
				.getContext()
				.getAuthentication()
				.getName();
		
		return getUserByEmail(email);
	}
	
	@Override
	@Transactional
	public void deleteUser(UUID id) {
		userRepository.deleteById(id);
	}
	
	@Override
	public boolean checkUserExistence(String email) {
		return userRepository.existsByEmail(email);
	}
}
