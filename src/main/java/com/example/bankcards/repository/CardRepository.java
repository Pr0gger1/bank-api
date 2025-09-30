package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {
	boolean existsByNumber(String number);
	Optional<Card> findByOwnerAndId(User user, UUID id);
	
	Page<Card> findByOwner(User user, Pageable pageable);
	
	@Query("SELECT c FROM Card c WHERE c.number LIKE %:lastFourDigits%")
	Page<Card> findByLastFourDigits(String lastFourDigits, Pageable pageable);
	
	@Query("SELECT c FROM Card c WHERE c.owner.firstName LIKE %:firstName%")
	Page<Card> findByOwnerFirstName(String firstName, Pageable pageable);
	
	@Query("SELECT c FROM Card c WHERE c.owner.firstName LIKE %:firstName% OR c.owner.lastName LIKE %:lastName%")
	Page<Card> findByOwnerFirstNameOrLastName(String firstName, String lastName, Pageable pageable);
}
