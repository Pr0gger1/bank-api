package com.example.bankcards.service.impl;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.Constants;
import com.example.bankcards.util.mappers.UserMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;
	
	@Override
	public UserDto getUserById(UUID id) {
		Optional<User> userResponse = userRepository.findById(id);
		
		if (userResponse.isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND, Constants.USER_NOT_FOUND_ERROR_MESSAGE
			);
		}
		
		return  userMapper.userToUserDto(userResponse.get());
	}
	
	@Override
	public User getUserByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(
						() -> new ResponseStatusException(
								HttpStatus.NOT_FOUND,
								Constants.USER_NOT_FOUND_ERROR_MESSAGE
						)
				);
	}
	
	@Override
	@Transactional
	public UserDto createUser(RegisterRequest request) {
		String encodedPassword = passwordEncoder.encode(request.getPassword());
		
		User user = User.builder()
				.email(request.getEmail())
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.patronymic(request.getPatronymic())
				.password(encodedPassword)
				.role(Role.USER)
				.build();
		
		user = userRepository.save(user);
		
		return userMapper.userToUserDto(user);
	}
	
	@Override
	@Transactional
	public UserDto updateUser(UserDto userDto) {
		Optional<User> oldUserInstance = userRepository.findById(userDto.getId());
		
		if (oldUserInstance.isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND, Constants.USER_NOT_FOUND_ERROR_MESSAGE
			);
		}
	
		User oldUser = oldUserInstance.get();

		validateEmailExistence(oldUser.getEmail(), userDto.getEmail());
		validateUserRoleConstance(oldUser, userDto);
		
		User updatedUser = User.builder()
				.id(oldUser.getId())
				.email(userDto.getEmail())
				.firstName(userDto.getFirstName())
				.lastName(userDto.getLastName())
				.patronymic(userDto.getPatronymic())
				.role(oldUser.getRole())
				.build();
		
		userRepository.save(updatedUser);
		
		return userMapper.userToUserDto(updatedUser);
	}
	
	private void validateEmailExistence(String oldEmail, String newEmail) {
		if (oldEmail.equals(newEmail)) {
			return;
		}
		
		if (userRepository.existsByEmail(newEmail)) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					String.format(Constants.USER_EMAIL_ALREADY_EXISTS_ERROR_MESSAGE, newEmail)
			);
		}
	}
	
	private void validateUserRoleConstance(User oldUser, UserDto newUser) {
		GrantedAuthority newRole = new SimpleGrantedAuthority(newUser.getRole().getAuthority());
		
		if (oldUser.getAuthorities().contains(newRole)) {
			return;
		}
		
		throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				Constants.USER_ROLE_CONSTANCE_ERROR_MESSAGE
		);
	}
	
	@Override
	public Page<UserDto> getUsers(int page, int size, String q) {
		Pageable pageable = PageRequest.of(page, size);
		
		if (q == null || q.isEmpty()) {
			Page<User> userPage = userRepository.findAll(pageable);
			return userPage.map(userMapper::userToUserDto);
		}
		
		List<String> searchItems = Arrays.stream(q.trim().split(" ")).toList();
		
		if (searchItems.size() > 1) {
			return userRepository.findByFirstNameOrLastName(
						searchItems.get(0),
						searchItems.get(1),
						pageable
					)
					.map(userMapper::userToUserDto);
		}
		
		return userRepository.findByFirstName(searchItems.get(0), pageable)
				.map(userMapper::userToUserDto);
	}
	
	@Override
	public UserDto getCurrentUser() {
		String email = SecurityContextHolder
				.getContext()
				.getAuthentication()
				.getName();
		
		User user = getUserByEmail(email);
		
		return userMapper.userToUserDto(user);
	}
	
	@Override
	@Transactional
	public void deleteUser(UUID id) {
		if (!userRepository.existsById(id)) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND,
					Constants.USER_NOT_FOUND_ERROR_MESSAGE
			);
		}
		
		userRepository.deleteById(id);
	}
	
	@Override
	public boolean checkUserExistence(String email) {
		return userRepository.existsByEmail(email);
	}
}
