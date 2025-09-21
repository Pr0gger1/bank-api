package com.example.bankcards.dto;

import com.example.bankcards.util.Constants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignInRequest {
	@Email
	@NotBlank
	private String email;
	
	@Size(min = 8, message = Constants.PASSWORD_VALIDATION_MESSAGE)
	private String password;
}
