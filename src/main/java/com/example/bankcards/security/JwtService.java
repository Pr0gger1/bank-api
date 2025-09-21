package com.example.bankcards.security;

import com.example.bankcards.entity.AuthToken;
import com.example.bankcards.repository.AuthTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {
	private static final String ROLE_CLAIM = "roles";
	private static final String EMAIL_CLAIM = "email";
	
	private final SecretKey accessSecret;
	private final SecretKey refreshSecret;
	private final AuthTokenRepository authTokenRepository;
	
	@Value("${jwt.expiration.accessInMinutes}")
	private int accessExpirationInMinutes;
	
	@Value("${jwt.expiration.refreshInMinutes}")
	private int refreshExpirationInMinutes;
	
	@Autowired
	public JwtService(
		@Qualifier("accessSecret")
		SecretKey accessSecret,
		
		@Qualifier("refreshSecret")
		SecretKey refreshSecret,
		AuthTokenRepository authTokenRepository) {
		this.accessSecret = accessSecret;
		this.refreshSecret = refreshSecret;
		this.authTokenRepository = authTokenRepository;
	}
	
	public String generateAccessToken(UserDetails userDetails) {
		Date expirationDate = getExpirationDate(accessExpirationInMinutes);
		
		return Jwts.builder()
				.subject(userDetails.getUsername())
				.expiration(expirationDate)
				.signWith(accessSecret)
				.claim(ROLE_CLAIM, userDetails.getAuthorities())
				.claim(EMAIL_CLAIM, userDetails.getUsername())
				.compact();
	}
	
	private Date getExpirationDate(int expirationInMinutes) {
		Instant expirationInstant = Instant.now()
				.plus(expirationInMinutes, ChronoUnit.MINUTES);
		
		return Date.from(expirationInstant);
	}
	
	public String generateRefreshToken(UserDetails userDetails) {
		Date expirationDate = getExpirationDate(refreshExpirationInMinutes);
		
		return Jwts.builder()
				.subject(userDetails.getUsername())
				.claim(ROLE_CLAIM, userDetails.getAuthorities())
				.expiration(expirationDate)
				.signWith(refreshSecret)
				.compact();
	}
	
	public boolean validateAccessToken(String token) {
		return validateToken(token, accessSecret);
	}
	
	private boolean validateToken(String token, SecretKey secretKey) {
		try {
			Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(token);
			
			return true;
		}
		catch (ExpiredJwtException e) {
			log.error("JwtService[validateToken]: expired token", e);
		}
		catch (UnsupportedJwtException e) {
			log.error("JwtService[validateToken]: unsupported token", e);
		}
		catch (MalformedJwtException e) {
			log.error("JwtService[validateToken]: malformed token", e);
		}
		catch (SignatureException e) {
			log.error("JwtService[validateToken]: signature exception", e);
		}
		catch (Exception e) {
			log.error("JwtService[validateToken]: invalid token", e);
		}
		
		return false;
	}
	
	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}
	
	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}
	
	private Claims extractAllClaims(String token) {
		return Jwts
				.parser()
				.verifyWith(accessSecret)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
	
	public boolean validateRefreshToken(String token) {
		return validateToken(token, refreshSecret);
	}
	
	public AuthToken verifyRefreshToken(String refreshToken) {
		Optional<AuthToken> token = authTokenRepository.findByToken(refreshToken);
		
		if (token.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found");
		}
		
		AuthToken authToken = token.get();
		
		if (token.get().getExpirationDate().compareTo(Instant.now()) < 0) {
			authTokenRepository.delete(authToken);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
		}
		
		return token.get();
	}
	
	public Claims getAccessClaims(String token) {
		return getClaims(token, accessSecret);
	}
	
	private Claims getClaims(String token, SecretKey secretKey) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseUnsecuredClaims(token)
				.getPayload();
	}
	
	public Claims getRefreshClaims(String token) {
		return getClaims(token, refreshSecret);
	}
}
