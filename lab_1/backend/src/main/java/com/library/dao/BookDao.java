package com.library.dao;

import com.library.model.Author;
import com.library.model.Book;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

public class BookDao extends BaseDao<Book, Long> {
    private static final Logger log = LogManager.getLogger(BookDao.class);

    @Override
    public Optional<Book> findById(Long id) {
        String sql = """
            SELECT b.id, b.title, b.isbn, b.year, b.total_copies, b.available_copies,
                   a.id AS a_id, a.full_name, a.country
            FROM books b
            LEFT JOIN book_authors ba ON ba.book_id = b.id
            LEFT JOIN authors a ON a.id = ba.author_id
            WHERE b.id = ?
            """;
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                Book book = null;
                while (rs.next()) {
                    if (book == null) book = mapBook(rs);
                    long aid = rs.getLong("a_id");
                    if (!rs.wasNull()) {
                        book.getAuthors().add(Author.builder()
                                .id(aid)
                                .fullName(rs.getString("full_name"))
                                .country(rs.getString("country"))
                                .build());
                    }
                }
                return Optional.ofNullable(book);
            }
        } catch (SQLException e) {
            log.error("findById failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Book> findAll() { return search(null); }

    /** Search by title / ISBN / author name (case-insensitive substring). */
    public List<Book> search(String query) {
        boolean hasQuery = query != null && !query.isBlank();
        String sql = """
            SELECT b.id, b.title, b.isbn, b.year, b.total_copies, b.available_copies,
                   a.id AS a_id, a.full_name, a.country
            FROM books b
            LEFT JOIN book_authors ba ON ba.book_id = b.id
            LEFT JOIN authors a ON a.id = ba.author_id
            """ + (hasQuery
                ? "WHERE LOWER(b.title) LIKE ? OR LOWER(COALESCE(b.isbn,'')) LIKE ? " +
                  "   OR b.id IN (SELECT ba2.book_id FROM book_authors ba2 " +
                  "               JOIN authors a2 ON a2.id=ba2.author_id " +
                  "               WHERE LOWER(a2.full_name) LIKE ?) "
                : "")
            + "ORDER BY b.id";
        Map<Long, Book> books = new LinkedHashMap<>();
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (hasQuery) {
                String like = "%" + query.toLowerCase() + "%";
                ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong("id");
                Book b = books.computeIfAbsent(id, k -> {
                    try { return mapBook(rs); } catch (SQLException ex) { throw new RuntimeException(ex); }
                });
                long aid = rs.getLong("a_id");
                if (!rs.wasNull()) {
                    b.getAuthors().add(Author.builder()
                            .id(aid).fullName(rs.getString("full_name"))
                            .country(rs.getString("country")).build());
                }
            }
            }
        } catch (SQLException e) {
            log.error("search failed", e);
            throw new RuntimeException(e);
        }
        return new ArrayList<>(books.values());
    }

    @Override
    public Book save(Book b) {
        String sql = b.getId() == null
                ? "INSERT INTO books(title, isbn, year, total_copies, available_copies) VALUES (?,?,?,?,?) RETURNING id"
                : "UPDATE books SET title=?, isbn=?, year=?, total_copies=?, available_copies=? WHERE id=? RETURNING id";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, b.getTitle());
            ps.setString(2, b.getIsbn());
            if (b.getYear() != null) ps.setInt(3, b.getYear()); else ps.setNull(3, Types.INTEGER);
            ps.setInt(4, b.getTotalCopies() == null ? 1 : b.getTotalCopies());
            ps.setInt(5, b.getAvailableCopies() == null ? 1 : b.getAvailableCopies());
            if (b.getId() != null) ps.setLong(6, b.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) b.setId(rs.getLong(1));
            }
            return b;
        } catch (SQLException e) {
            log.error("save failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM books WHERE id=?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("delete failed", e);
            throw new RuntimeException(e);
        }
    }

    public boolean changeAvailable(Connection c, long bookId, int delta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE books SET available_copies = available_copies + ? " +
                "WHERE id=? AND available_copies + ? >= 0 AND available_copies + ? <= total_copies")) {
            ps.setInt(1, delta);
            ps.setLong(2, bookId);
            ps.setInt(3, delta);
            ps.setInt(4, delta);
            return ps.executeUpdate() > 0;
        }
    }

    private Book mapBook(ResultSet rs) throws SQLException {
        return Book.builder()
                .id(rs.getLong("id"))
                .title(rs.getString("title"))
                .isbn(rs.getString("isbn"))
                .year((Integer) rs.getObject("year"))
                .totalCopies(rs.getInt("total_copies"))
                .availableCopies(rs.getInt("available_copies"))
                .authors(new ArrayList<>())
                .build();
    }
}
