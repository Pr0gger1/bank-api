package com.example.bankcards.service;

import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;

import java.util.UUID;

public interface RefreshTokenService {
	RefreshToken generateRefreshToken(UUID userId);
	RefreshToken generateRefreshToken(User user);
	void deleteByUserId(UUID id);
	void deleteByUser(User user);
	RefreshToken verifyRefreshToken(String refreshToken);
}
