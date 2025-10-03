package com.example.bankcards.service.impl;

import com.example.bankcards.util.JwtUtils;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {
	public static final String ROLE_CLAIM = "roles";
	public static final String EMAIL_CLAIM = "email";
	
	private final SecretKey accessSecret;
	private final SecretKey refreshSecret;
	private final JwtCookieService jwtCookieService;
	
	@Value("${jwt.expiration.accessInMs}")
	private int accessExpirationInMs;
	
	@Value("${jwt.expiration.refreshInMs}")
	private int refreshExpirationInMs;
	
	@Value("${jwt.cookie.refresh.name}")
	private String refreshJwtCookie;
	
	@Autowired
	public JwtService(
		@Qualifier("accessSecret")
		SecretKey accessSecret,
		
		@Qualifier("refreshSecret")
		SecretKey refreshSecret,
		JwtCookieService jwtCookieService
	) {
		this.accessSecret = accessSecret;
		this.refreshSecret = refreshSecret;
		this.jwtCookieService = jwtCookieService;
	}
	
	public String generateAccessToken(UserDetails userDetails) {
		return JwtUtils.generateToken(userDetails, accessExpirationInMs, accessSecret);
	}
	
	public ResponseCookie generateRefreshJwtCookie(String refreshToken) {
		return jwtCookieService.generateCookie(refreshJwtCookie, refreshToken, "/api/auth/refresh", refreshExpirationInMs / 1000);
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
}
