package com.example.bankcards.util;

import java.util.List;

public final class Constants {
	public static final List<String> ENDPOINT_WHITELIST = List.of("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**");
	public static final String BEARER_PREFIX = "Bearer ";
	
	public static final String BLACKLIST_CACHE_NAME = "jwt-blacklist";
	
	public static final String NOT_ENOUGH_MONEY_ERROR_MESSAGE = "Not enough money on sender card";
	public static final String USER_WITH_EMAIL_NOT_FOUND_ERROR_MESSAGE = "User with email %s not found";
	public static final String USER_ROLE_CONSTANCE_ERROR_MESSAGE = "User role can't be changed";
	public static final String USER_EMAIL_ALREADY_EXISTS_ERROR_MESSAGE = "User with email %s already exists";
	public static final String CARD_NOT_FOUND_ERROR_MESSAGE = "Card not found";
	public static final String USER_NOT_FOUND_ERROR_MESSAGE = "User not found";
	public static final String USER_DOESNT_HAVE_CARD_ERROR_MESSAGE = "User %s %s don't has card with id %s";
	public static final String GET_CARD_FORBIDDEN_ERROR_MESSAGE = "You don't have rights to view this card";
	public static final String PASSWORD_VALIDATION_MESSAGE = "Password must have a minimum of 8 and maximum of 255 characters";
	public static final String TRANSACTION_MAKE_SUCCESS_MESSAGE = "Transaction made successfully";
	public static final String UNAUTHORIZED_ERROR_MESSAGE = "You're not authorized to access this resource.";
}
