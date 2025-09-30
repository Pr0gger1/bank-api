package com.example.bankcards.service;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface UserService {
	UserDto getUserById(UUID id);
	User getUserByEmail(String email);
	UserDto createUser(RegisterRequest request);
	UserDto updateUser(UserDto userDto);
	Page<UserDto> getUsers(int page, int size, String search);
	UserDto getCurrentUser();
	void deleteUser(UUID id);
	boolean checkUserExistence(String email);
}
