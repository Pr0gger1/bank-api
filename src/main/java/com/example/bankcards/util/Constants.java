package com.example.bankcards.util;

import java.util.List;

public final class Constants {
	public static final List<String> ENDPOINT_WHITELIST = List.of("/api/auth/**");
	public static final String BEARER_PREFIX = "Bearer ";
	public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
	
	public static final String BLACKLIST_CACHE_NAME = "jwt-blacklist";
	
	public static final String PASSWORD_VALIDATION_MESSAGE = "Password must have a minimum of 8 and maximum of 255 characters";
	public static final String USERNAME_VALIDATION_MESSAGE = "Username must have a minimum of 4 characters and a maximum of 50 characters";
	public static final String EMAIL_VALIDATION_MESSAGE = "Email must be a valid email address";
	
	public static final String UNAUTHORIZED_ERROR_MESSAGE = "You're not authorized to access this resource.";
}
