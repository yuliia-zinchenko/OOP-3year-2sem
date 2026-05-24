package com.library.controller;

import com.library.dto.UserDto;
import com.library.mapper.UserMapper;
import com.library.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final CurrentUserService currentUser;
    private final UserMapper mapper;

    @GetMapping("/me")
    public UserDto me() {
        return mapper.toDto(currentUser.current());
    }
}
