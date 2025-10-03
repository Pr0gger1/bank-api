package com.example.bankcards.repository;

import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
	Optional<RefreshToken> findByToken(String token);
	void deleteByUser(User user);
}
