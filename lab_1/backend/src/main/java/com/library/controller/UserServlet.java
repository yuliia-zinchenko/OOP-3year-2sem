package com.library.controller;

import com.library.dto.UserDto;
import com.library.mapper.UserMapper;
import com.library.model.User;
import com.library.util.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mapstruct.factory.Mappers;

import java.io.IOException;

/** GET /api/me -> current user (extracted from JWT by JwtFilter). */
@WebServlet(urlPatterns = "/api/me")
public class UserServlet extends HttpServlet {
    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = (User) req.getAttribute("currentUser");
        UserDto dto = mapper.toDto(u);
        Json.write(resp, 200, dto);
    }
}
