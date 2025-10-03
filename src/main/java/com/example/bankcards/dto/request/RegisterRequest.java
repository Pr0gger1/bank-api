package com.example.bankcards.dto.request;

import com.example.bankcards.util.Constants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
	@NotNull(message = "Email is required")
	@Email
	private String email;
	
	@NotNull(message = "Password is required")
	@Size(min = 8, max = 255, message = Constants.PASSWORD_VALIDATION_MESSAGE)
	private String password;
	
	@NotNull(message = "First name is required")
	private String firstName;
	
	@NotNull(message = "Last name is required")
	private String lastName;
	private String patronymic;
}
