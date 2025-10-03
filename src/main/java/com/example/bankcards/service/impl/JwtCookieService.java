package com.example.bankcards.service.impl;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

@Service
public class JwtCookieService {
	@Value("${jwt.cookie.refresh.name}")
	private String refreshJwtCookie;
	
	public ResponseCookie getCleanJwtRefreshCookie() {
		return ResponseCookie.from(refreshJwtCookie, "").path("/api/auth/refresh").build();
	}
	
	public String getRefreshJwtFromCookies(HttpServletRequest request) {
		return getCookieValueByName(request, refreshJwtCookie);
	}
	
	public String getCookieValueByName(HttpServletRequest request, String name) {
		Cookie cookie = WebUtils.getCookie(request, name);

		if (cookie == null) {
			return null;
		}
		
		return cookie.getValue();
	}
	
	public ResponseCookie generateCookie(String name, String value, String path, long maxAgeInSeconds) {
		return ResponseCookie
				.from(name, value)
				.path(path)
				.maxAge(maxAgeInSeconds)
				.httpOnly(true)
				.build();
	}
}
