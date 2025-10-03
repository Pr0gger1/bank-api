package com.example.bankcards.dto.request;

import com.example.bankcards.util.Constants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
	@Email
	@NotBlank
	private String email;
	
	@Size(min = 8, message = Constants.PASSWORD_VALIDATION_MESSAGE)
	private String password;
}
