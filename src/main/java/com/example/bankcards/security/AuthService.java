package com.example.bankcards.security;

import com.example.bankcards.dto.SignInRequest;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.AuthToken;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import com.example.bankcards.enums.TokenType;
import com.example.bankcards.repository.AuthTokenRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {
	private final JwtService jwtService;
	private final UserService userService;
	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final AuthTokenRepository authTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenBlacklistService tokenBlacklistService;
	
	@Value("${jwt.expiration.refreshInMinutes}")
	private int refreshExpirationInMinutes;
	
	public JwtResponse login(SignInRequest signInRequest) {
		String email = signInRequest.getEmail();
		String password = signInRequest.getPassword();
		
		return authenticate(email, password);
	}
	
	private JwtResponse authenticate(
			String email,
			String password
	) {
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(email, password);
		authenticationManager.authenticate(token);
		
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		
		String accessToken = jwtService.generateAccessToken(user);
		String refreshToken = jwtService.generateRefreshToken(user);
		
		AuthToken authToken = AuthToken.builder()
				.user(user)
				.token(refreshToken)
				.expirationDate(Instant.now().plus(refreshExpirationInMinutes, ChronoUnit.MINUTES))
				.tokenType(TokenType.REFRESH)
				.build();
		
		authTokenRepository.save(authToken);
		log.debug("AuthService[buildToken][1]: token has been saved");
		
		return JwtResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.build();
	}
	
	public JwtResponse refreshToken(String refreshToken) {
		AuthToken authToken = jwtService.verifyRefreshToken(refreshToken);
		
		if (authToken.isExpired() || authToken.isRevoked() || !jwtService.validateRefreshToken(refreshToken)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
		}
		
		authToken.setRevoked(true);
		authToken.setExpired(true);
		
		authTokenRepository.save(authToken);
		
		User user = authToken.getUser();
		
		return authenticate(user.getEmail(), user.getPassword());
	}
	
	public void logout(String authHeader) {
		String accessToken = authHeader.substring(Constants.BEARER_PREFIX.length()).trim();

		if (!jwtService.validateAccessToken(accessToken)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
		}
		
		String email = jwtService.extractUsername(accessToken);
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		
		Optional<AuthToken> optionalRefreshToken = authTokenRepository.findByUser(user);
		
		optionalRefreshToken.ifPresent(token -> {
			token.setRevoked(true);
			token.setExpired(true);
			
			tokenBlacklistService.blackListJwt(accessToken);
			authTokenRepository.save(token);
		});
	}
	
	public JwtResponse register(UserDto userDto) {
		if (userService.checkUserExistence(userDto.getEmail())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists");
		}
		
		try {
			String encodedPassword = passwordEncoder.encode(userDto.getPassword());
			
			User user = User.builder()
					.email(userDto.getEmail())
					.firstName(userDto.getFirstName())
					.lastName(userDto.getLastName())
					.patronymic(userDto.getPatronymic())
					.password(encodedPassword)
					.role(Role.USER)
					.build();
			
			userRepository.save(user);
			
			log.debug("AuthService[register][1]: user has been saved");
			
			return authenticate(userDto.getEmail(), userDto.getPassword());
		}
		catch (InvalidDataAccessApiUsageException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error");
		}
		catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}
}
