package com.example.bankcards.service;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.impl.UserServiceImpl;
import com.example.bankcards.util.Constants;
import com.example.bankcards.util.mappers.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
	
	@Mock
	private UserRepository userRepository;
	
	@Mock
	private PasswordEncoder passwordEncoder;
	
	@Mock
	private UserMapper userMapper;
	
	@InjectMocks
	private UserServiceImpl userService;
	
	private UUID userId;
	private User user;
	private UserDto userDto;
	
	@BeforeEach
	void setUp() {
		userId = UUID.randomUUID();
		user = User.builder()
				.id(userId)
				.email("test@example.com")
				.firstName("John")
				.lastName("Doe")
				.role(Role.USER)
				.password("encoded")
				.build();
		
		userDto = UserDto.builder()
				.id(userId)
				.email("test@example.com")
				.firstName("John")
				.lastName("Doe")
				.role(Role.USER)
				.build();
	}
	
	@Test
	void getUserById_ShouldReturnUser_WhenExists() {
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(userMapper.userToUserDto(user)).thenReturn(userDto);
		
		UserDto result = userService.getUserById(userId);
		
		assertThat(result).isEqualTo(userDto);
	}
	
	@Test
	void getUserById_ShouldThrow_WhenNotFound() {
		when(userRepository.findById(userId)).thenReturn(Optional.empty());
		
		assertThatThrownBy(() -> userService.getUserById(userId))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining(Constants.USER_NOT_FOUND_ERROR_MESSAGE);
	}
	
	@Test
	void getUserByEmail_ShouldReturnUser_WhenExists() {
		when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
		
		User result = userService.getUserByEmail("test@example.com");
		
		assertThat(result).isEqualTo(user);
	}
	
	@Test
	void getUserByEmail_ShouldThrow_WhenNotFound() {
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
		
		assertThatThrownBy(() -> userService.getUserByEmail("missing@example.com"))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("User with email missing@example.com not found");
	}
	
	@Test
	void createUser_ShouldEncodePasswordAndSave() {
		RegisterRequest request = RegisterRequest.builder()
				.email("new@example.com")
				.password("pwd")
				.firstName("Alice")
				.lastName("Smith")
				.patronymic(null)
				.build();
		
		when(passwordEncoder.encode("pwd")).thenReturn("encodedPwd");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(userMapper.userToUserDto(any(User.class))).thenReturn(userDto);
		
		UserDto result = userService.createUser(request);
		
		assertThat(result).isEqualTo(userDto);
		verify(passwordEncoder).encode("pwd");
		verify(userRepository).save(any(User.class));
	}
	
	@Test
	void updateUser_ShouldUpdateUser_WhenExistsAndValid() {
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(userMapper.userToUserDto(any(User.class))).thenReturn(userDto);
		
		UserDto result = userService.updateUser(userDto);
		
		assertThat(result).isEqualTo(userDto);
	}
	
	@Test
	void updateUser_ShouldThrow_WhenUserNotFound() {
		when(userRepository.findById(userId)).thenReturn(Optional.empty());
		
		assertThatThrownBy(() -> userService.updateUser(userDto))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining(Constants.USER_NOT_FOUND_ERROR_MESSAGE);
	}
	
	@Test
	void updateUser_ShouldThrow_WhenEmailAlreadyExists() {
		UserDto dto = UserDto.builder()
				.id(userId)
				.email("new@example.com")
				.role(Role.USER)
				.build();
		
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(userRepository.existsByEmail("new@example.com")).thenReturn(true);
		
		assertThatThrownBy(() -> userService.updateUser(dto))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("already exists");
	}
	
	@Test
	void getUsers_ShouldReturnAll_WhenNoQuery() {
		Page<User> page = new PageImpl<>(List.of(user));
		when(userRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);
		when(userMapper.userToUserDto(user)).thenReturn(userDto);
		
		Page<UserDto> result = userService.getUsers(0, 10, null);
		
		assertThat(result.getContent()).containsExactly(userDto);
	}
	
	@Test
	void getUsers_ShouldSearchByFirstName_WhenSingleQuery() {
		Page<User> page = new PageImpl<>(List.of(user));
		when(userRepository.findByFirstName(eq("John"), any(Pageable.class))).thenReturn(page);
		when(userMapper.userToUserDto(user)).thenReturn(userDto);
		
		Page<UserDto> result = userService.getUsers(0, 10, "John");
		
		assertThat(result.getContent()).containsExactly(userDto);
	}
	
	@Test
	void getUsers_ShouldSearchByFirstNameOrLastName_WhenTwoWordsQuery() {
		Page<User> page = new PageImpl<>(List.of(user));
		when(userRepository.findByFirstNameOrLastName(eq("John"), eq("Doe"), any(Pageable.class))).thenReturn(page);
		when(userMapper.userToUserDto(user)).thenReturn(userDto);
		
		Page<UserDto> result = userService.getUsers(0, 10, "John Doe");
		
		assertThat(result.getContent()).containsExactly(userDto);
	}
	
	@Test
	void getCurrentUser_ShouldReturnUserFromSecurityContext() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("test@example.com", null)
		);
		
		when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
		when(userMapper.userToUserDto(user)).thenReturn(userDto);
		
		UserDto result = userService.getCurrentUser();
		
		assertThat(result).isEqualTo(userDto);
	}
	
	@Test
	void deleteUser_ShouldDelete_WhenExists() {
		when(userRepository.existsById(userId)).thenReturn(true);
		
		userService.deleteUser(userId);
		
		verify(userRepository).deleteById(userId);
	}
	
	@Test
	void deleteUser_ShouldThrow_WhenNotExists() {
		when(userRepository.existsById(userId)).thenReturn(false);
		
		assertThatThrownBy(() -> userService.deleteUser(userId))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining(Constants.USER_NOT_FOUND_ERROR_MESSAGE);
	}
	
	@Test
	void checkUserExistence_ShouldReturnTrue_WhenExists() {
		when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
		
		boolean result = userService.checkUserExistence("test@example.com");
		
		assertThat(result).isTrue();
	}
	
	@Test
	void checkUserExistence_ShouldReturnFalse_WhenNotExists() {
		when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
		
		boolean result = userService.checkUserExistence("test@example.com");
		
		assertThat(result).isFalse();
	}
}
