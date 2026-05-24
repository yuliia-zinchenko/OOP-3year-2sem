package com.library.dao;

import com.library.model.Author;

import java.sql.*;
import java.util.*;

public class AuthorDao extends BaseDao<Author, Long> {

    @Override
    public Optional<Author> findById(Long id) {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM authors WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Author> findAll() {
        List<Author> list = new ArrayList<>();
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM authors ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public Author save(Author a) {
        String sql = a.getId() == null
                ? "INSERT INTO authors(full_name, country) VALUES (?,?) RETURNING id"
                : "UPDATE authors SET full_name=?, country=? WHERE id=? RETURNING id";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, a.getFullName());
            ps.setString(2, a.getCountry());
            if (a.getId() != null) ps.setLong(3, a.getId());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) a.setId(rs.getLong(1)); }
            return a;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean deleteById(Long id) {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM authors WHERE id=?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Author map(ResultSet rs) throws SQLException {
        return Author.builder()
                .id(rs.getLong("id"))
                .fullName(rs.getString("full_name"))
                .country(rs.getString("country"))
                .build();
    }
}
