package com.library.dao;

import com.library.model.Loan;
import com.library.model.LoanStatus;
import com.library.model.LoanType;

import java.sql.*;
import java.util.*;

public class LoanDao extends BaseDao<Loan, Long> {

    @Override
    public Optional<Loan> findById(Long id) {
        return querySingle("SELECT * FROM loans WHERE id=?", ps -> ps.setLong(1, id));
    }

    @Override
    public List<Loan> findAll() {
        return queryList("SELECT * FROM loans ORDER BY id DESC", ps -> {});
    }

    public List<Loan> findByUser(long userId) {
        return queryList("SELECT * FROM loans WHERE user_id=? ORDER BY id DESC",
                ps -> ps.setLong(1, userId));
    }

    public List<Loan> findByStatus(LoanStatus status) {
        return queryList("SELECT * FROM loans WHERE status=? ORDER BY id DESC",
                ps -> ps.setString(1, status.name()));
    }

    @Override
    public Loan save(Loan l) {
        // Used only for inserts here (full updates go through LoanService transactions).
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO loans(user_id, book_id, status, type) " +
                     "VALUES (?,?,?,?) RETURNING id, ordered_at")) {
            ps.setLong(1, l.getUserId());
            ps.setLong(2, l.getBookId());
            ps.setString(3, l.getStatus() == null ? "ORDERED" : l.getStatus().name());
            ps.setString(4, l.getType() == null ? null : l.getType().name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    l.setId(rs.getLong(1));
                    l.setOrderedAt(rs.getTimestamp(2).toLocalDateTime());
                }
            }
            return l;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean deleteById(Long id) {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM loans WHERE id=?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Optional<Loan> querySingle(String sql, SqlSetter setter) {
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private List<Loan> queryList(String sql, SqlSetter setter) {
        List<Loan> out = new ArrayList<>();
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    private Loan map(ResultSet rs) throws SQLException {
        return Loan.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .bookId(rs.getLong("book_id"))
                .librarianId((Long) rs.getObject("librarian_id"))
                .status(LoanStatus.valueOf(rs.getString("status")))
                .type(rs.getString("type") == null ? null : LoanType.valueOf(rs.getString("type")))
                .orderedAt(ts(rs, "ordered_at"))
                .issuedAt(ts(rs, "issued_at"))
                .dueAt(ts(rs, "due_at"))
                .returnedAt(ts(rs, "returned_at"))
                .build();
    }

    private static java.time.LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    @FunctionalInterface
    private interface SqlSetter { void set(PreparedStatement ps) throws SQLException; }
}
