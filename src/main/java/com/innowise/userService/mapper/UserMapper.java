package com.innowise.userService.mapper;

import com.innowise.userService.model.dto.UserDto;
import com.innowise.userService.model.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDTO(User user);
    User toEntity(UserDto userDTO);
}