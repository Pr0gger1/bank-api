package com.example.bankcards.service;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface UserService {
	User getUserById(UUID id);
	User getUserByEmail(String email);
	User createUser(RegisterRequest request);
	UserDto updateUser(UserDto userDto);
	Page<User> getUsers(int page, int size);
	User getCurrentUser();
	void deleteUser(UUID id);
	boolean checkUserExistence(String email);
}
