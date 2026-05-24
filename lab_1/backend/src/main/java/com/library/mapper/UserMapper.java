package com.library.mapper;

import com.library.dto.UserDto;
import com.library.model.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {
    UserDto toDto(User user);
}
