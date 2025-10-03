package com.example.bankcards.entity;

import com.example.bankcards.enums.TokenType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tokens")
public class RefreshToken {
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	private UUID id;
	
	@Column(nullable = false, unique = true)
	private String token;
	
	@Enumerated(EnumType.STRING)
	private TokenType tokenType;
	
	private Instant expirationDate;
	
	@Builder.Default
	private boolean revoked = false;
	
	@Builder.Default
	private boolean expired = false;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id")
	private User user;
}
