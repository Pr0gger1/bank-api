package com.example.bankcards.util.mappers;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
	UserDto userToUserDto(User user);
	User userDtoToUser(UserDto userDto);
}
