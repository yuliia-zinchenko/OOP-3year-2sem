package com.library.service;

import com.library.dao.UserDao;
import com.library.model.User;

public class UserService {
    private final UserDao userDao;

    public UserService(UserDao userDao) { this.userDao = userDao; }

    /** Find user by JWT 'sub' or auto-create on first login. */
    public User findOrCreate(String sub, String email, String fullName) {
        return userDao.findBySub(sub).orElseGet(() ->
                userDao.save(User.builder()
                        .sub(sub).email(email).fullName(fullName).role("READER").build()));
    }

    public User get(Long id) { return userDao.findById(id).orElseThrow(); }
}
