package com.example.bankcards.security;

import com.example.bankcards.util.Constants;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {
	@CachePut(value = Constants.BLACKLIST_CACHE_NAME, key = "#jwt")
	public String blackListJwt(String jwt) {
		return jwt;
	}
	
	@Cacheable(value = Constants.BLACKLIST_CACHE_NAME, unless = "#jwt == null")
	public String getJwtBlacklist(String jwt) {
		return null;
	}
}
