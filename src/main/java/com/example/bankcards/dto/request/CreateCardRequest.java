package com.example.bankcards.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CreateCardRequest {
	@NotNull(message = "User id cannot be null")
	private UUID user_id;
	
	@Positive
	@NotNull(message = "Period cannot be null")
	private int period;
}
