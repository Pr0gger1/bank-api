package com.example.bankcards.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CreateCardRequest {
	@JsonProperty("user_id")
	@NotNull(message = "User id cannot be null")
	private UUID userId;
	
	@JsonProperty("period")
	@Positive
	@NotNull(message = "Period cannot be null")
	private int periodInYears;
}
