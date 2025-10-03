package com.example.bankcards.dto.response;

import com.example.bankcards.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseCookie;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
	private String accessToken;
	private ResponseCookie refreshCookie;
	private User user;
}
