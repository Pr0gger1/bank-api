package com.example.bankcards.service.impl;

import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.TokenType;
import com.example.bankcards.repository.RefreshTokenRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.RefreshTokenService;
import com.example.bankcards.util.Constants;
import com.example.bankcards.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	
	private final SecretKey refreshSecret;
	
	@Value("${jwt.expiration.refreshInMs}")
	private long refreshExpirationInMs;
	
	RefreshTokenServiceImpl(
		RefreshTokenRepository refreshTokenRepository,
		UserRepository userRepository,
		@Qualifier("refreshSecret") SecretKey refreshSecret
	) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.userRepository = userRepository;
		this.refreshSecret = refreshSecret;
	}
	
	@Override
	public RefreshToken generateRefreshToken(UUID userId) {
		Optional<User> optionalUser = userRepository.findById(userId);
		
		if (optionalUser.isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.UNAUTHORIZED,
					Constants.USER_NOT_FOUND_ERROR_MESSAGE
			);
		}
		
		User user = optionalUser.get();
		
		return generateRefreshToken(user);
	}
	
	@Override
	public RefreshToken generateRefreshToken(User user) {
		String refreshToken = JwtUtils.generateToken(user, refreshExpirationInMs, refreshSecret);
		
		RefreshToken authToken = RefreshToken.builder()
				.user(user)
				.token(refreshToken)
				.expirationDate(Instant.now().plusMillis(refreshExpirationInMs))
				.tokenType(TokenType.REFRESH)
				.build();
		
		refreshTokenRepository.save(authToken);
		log.debug("RefreshTokenService[generateRefreshToken][1]: token has been saved");
		
		return authToken;
	}
	
	@Override
	public void deleteByUserId(UUID id) {
		Optional<User> user = userRepository.findById(id);
		
		if (user.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, Constants.USER_NOT_FOUND_ERROR_MESSAGE);
		}
		
		refreshTokenRepository.deleteByUser(user.get());
	}
	
	@Override
	public void deleteByUser(User user) {
		if (!userRepository.existsById(user.getId())) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND,
					Constants.USER_NOT_FOUND_ERROR_MESSAGE
			);
		}
		
		refreshTokenRepository.deleteByUser(user);
	}
	
	@Override
	public RefreshToken verifyRefreshToken(String refreshToken) {
		Optional<RefreshToken> token = refreshTokenRepository.findByToken(refreshToken);
		
		if (token.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found");
		}
		
		RefreshToken authToken = token.get();
		Instant expirationDate = authToken.getExpirationDate();
		
		if (expirationDate.compareTo(Instant.now()) < 0) {
			refreshTokenRepository.delete(authToken);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
		}
		
		return token.get();
	}
}
