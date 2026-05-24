package com.library.dao;

import com.library.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

public class UserDao extends BaseDao<User, Long> {
    private static final Logger log = LogManager.getLogger(UserDao.class);

    @Override
    public Optional<User> findById(Long id) {
        return querySingle("SELECT * FROM users WHERE id=?", ps -> ps.setLong(1, id));
    }

    public Optional<User> findBySub(String sub) {
        return querySingle("SELECT * FROM users WHERE sub=?", ps -> ps.setString(1, sub));
    }

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM users ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public User save(User u) {
        String sql = u.getId() == null
                ? "INSERT INTO users(sub,email,full_name,role) VALUES (?,?,?,?) RETURNING id, created_at"
                : "UPDATE users SET sub=?, email=?, full_name=?, role=? WHERE id=? RETURNING id, created_at";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getSub());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getFullName());
            ps.setString(4, u.getRole() == null ? "READER" : u.getRole());
            if (u.getId() != null) ps.setLong(5, u.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    u.setId(rs.getLong(1));
                    u.setCreatedAt(rs.getTimestamp(2).toLocalDateTime());
                }
            }
            return u;
        } catch (SQLException e) {
            log.error("save user failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Optional<User> querySingle(String sql, SqlSetter setter) {
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private User map(ResultSet rs) throws SQLException {
        return User.builder()
                .id(rs.getLong("id"))
                .sub(rs.getString("sub"))
                .email(rs.getString("email"))
                .fullName(rs.getString("full_name"))
                .role(rs.getString("role"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }

    @FunctionalInterface
    private interface SqlSetter { void set(PreparedStatement ps) throws SQLException; }
}
