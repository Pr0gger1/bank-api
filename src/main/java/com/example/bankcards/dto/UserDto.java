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
	@NotNull(message = "User id cannot be null")
	private UUID id;
	
	@Email(message = "Invalid email")
	@NotNull(message = "User email cannot be null")
	private String email;
	
	@NotNull(message = "User first name cannot be null")
	private String firstName;
	
	@NotNull(message = "User last name cannot be null")
	private String lastName;
	
	@Nullable
	private String patronymic;
	
	private Role role;
}
