package com.example.bankcards.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class JwtConfig {
	@Value("${jwt.secret.access}")
	private String accessSecret;
	
	@Value("${jwt.secret.refresh}")
	private String refreshSecret;
	
	@Bean
	@Qualifier("accessSecret")
	public SecretKey accessSecretKey() {
		return Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecret));
	}
	
	@Bean
	@Qualifier("refreshSecret")
	public SecretKey refreshSecretKey() {
		return Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecret));
	}
}
