package com.example.bankcards.util;

import io.jsonwebtoken.Jwts;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;

import static com.example.bankcards.service.impl.JwtService.EMAIL_CLAIM;
import static com.example.bankcards.service.impl.JwtService.ROLE_CLAIM;

public class JwtUtils {
	public static String generateToken(UserDetails userDetails, long expirationTime, SecretKey signingKey) {
		Date expirationDate = getExpirationDate(expirationTime);
		
		String username = userDetails.getUsername();
		Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
		
		return Jwts.builder()
				.subject(username)
				.expiration(expirationDate)
				.signWith(signingKey)
				.claim(ROLE_CLAIM, authorities)
				.claim(EMAIL_CLAIM, username)
				.compact();
	}
	
	private static Date getExpirationDate(long expirationInMs) {
		Instant expirationInstant = Instant.now()
				.plusMillis(expirationInMs);
		
		return Date.from(expirationInstant);
	}
}
