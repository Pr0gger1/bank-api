package com.example.bankcards.util;

import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class UserUtils {
	public static boolean hasRole(User user, Role role) {
		Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
		
		return authorities.stream()
				.map(GrantedAuthority::getAuthority)
				.anyMatch(a -> a.equals(role.getAuthority()));
	}
}
