package com.example.bankcards.repository;

import com.example.bankcards.entity.AuthToken;
import com.example.bankcards.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {
	Optional<AuthToken> findByToken(String token);
	Optional<AuthToken> findByUser(User user);
}
