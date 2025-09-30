package com.example.bankcards.controller;

import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.UUID;

public class BaseControllerTest {
	protected final String token = "Bearer someToken";
	protected final UsernamePasswordAuthenticationToken userAuth =
			new UsernamePasswordAuthenticationToken(mockUser(), "password",
					AuthorityUtils.createAuthorityList("USER"));
	
	protected User mockUser() {
		return User.builder()
				.id(UUID.randomUUID())
				.email("test@example.com")
				.firstName("John")
				.lastName("Doe")
				.password("password123")
				.role(Role.USER)
				.build();
	}
}
