package com.library.mapper;

import com.library.domain.UserAccount;
import com.library.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(UserAccount user);
}
