package com.example.bankcards.repository;

import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByEmail(String email);
	boolean existsByEmail(String email);
	
	@Query("SELECT u FROM User u WHERE u.firstName LIKE %:firstName% OR u.lastName LIKE %:lastName%")
	Page<User> findByFirstNameOrLastName(String firstName, String lastName, Pageable pageable);
	
	@Query("SELECT u FROM User u WHERE u.firstName LIKE %:firstName%")
	Page<User> findByFirstName(String firstName, Pageable pageable);
}
