package com.example.bankcards.dto;

import com.example.bankcards.enums.Role;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserDto {
	@NotNull
	private UUID id;
	
	@Email
	private String email;
	
	@NotNull
	private String firstName;
	
	@NotNull
	private String lastName;
	
	@Nullable
	private String patronymic;
	
	private Role role;
}
