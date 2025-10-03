package com.example.bankcards.entity;

import com.example.bankcards.enums.Role;
import com.example.bankcards.util.Constants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Table(name = "users")
@Entity
public class User implements UserDetails {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(nullable = false, unique = true)
	@Email
	private String email;
	
	@Column(nullable = false)
	private String firstName;
	
	@Column(nullable = false)
	private String lastName;
	
	private String patronymic;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private Role role = Role.USER;
	
	@Column(nullable = false)
	@JsonIgnore
	@Size(min = 8, max = 255, message = Constants.PASSWORD_VALIDATION_MESSAGE)
	private String password;
	
	public boolean hasRole(Role role) {
		Collection<? extends GrantedAuthority> authorities = getAuthorities();
		
		return authorities.stream()
				.map(GrantedAuthority::getAuthority)
				.anyMatch(a -> a.equals(role.getAuthority()));
	}
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(role.getAuthority()));
	}
	
	@Override
	public String getUsername() {
		return email;
	}
}
