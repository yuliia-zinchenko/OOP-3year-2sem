package com.library.service;

import com.library.config.DatabaseConnection;
import com.library.dao.BookDao;
import com.library.dao.LoanDao;
import com.library.model.Loan;
import com.library.model.LoanStatus;
import com.library.model.LoanType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Loan lifecycle:
 *   READER:   order(...)        -> ORDERED  (reserves a copy)
 *   READER:   cancel(...)       -> CANCELLED (releases the copy)
 *   LIBRARIAN: issue(..., type) -> ISSUED   (sets due date for SUBSCRIPTION)
 *   LIBRARIAN: returnLoan(...)  -> RETURNED (releases the copy)
 */
public class LoanService {
    private static final Logger log = LogManager.getLogger(LoanService.class);
    private static final int SUBSCRIPTION_DAYS = 14;
    private static final int READING_HALL_HOURS = 8;

    private final LoanDao loanDao;
    private final BookDao bookDao;

    public LoanService(LoanDao loanDao, BookDao bookDao) {
        this.loanDao = loanDao;
        this.bookDao = bookDao;
    }

    public List<Loan> listForUser(long userId) { return loanDao.findByUser(userId); }
    public List<Loan> listByStatus(LoanStatus s) { return loanDao.findByStatus(s); }
    public List<Loan> listAll() { return loanDao.findAll(); }

    /** READER places an order — reserves one copy. */
    public Loan order(long userId, long bookId) {
        return tx(c -> {
            if (!bookDao.changeAvailable(c, bookId, -1))
                throw new IllegalStateException("Немає доступних примірників");
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO loans(user_id, book_id, status) VALUES (?,?, 'ORDERED') " +
                    "RETURNING id, ordered_at")) {
                ps.setLong(1, userId);
                ps.setLong(2, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    Loan l = Loan.builder()
                            .id(rs.getLong(1))
                            .userId(userId).bookId(bookId)
                            .status(LoanStatus.ORDERED)
                            .orderedAt(rs.getTimestamp(2).toLocalDateTime())
                            .build();
                    log.info("Reader {} ordered book {} (loan {})", userId, bookId, l.getId());
                    return l;
                }
            }
        });
    }

    /** READER cancels their own pending order (must still be ORDERED). */
    public Loan cancel(long loanId, long callerUserId) {
        return tx(c -> {
            Loan l = loanDao.findById(loanId).orElseThrow();
            if (l.getUserId() != callerUserId)
                throw new SecurityException("Чуже замовлення");
            if (l.getStatus() != LoanStatus.ORDERED)
                throw new IllegalStateException("Скасувати можна лише замовлення зі статусом ORDERED");
            update(c, "UPDATE loans SET status='CANCELLED' WHERE id=?", ps -> ps.setLong(1, loanId));
            bookDao.changeAvailable(c, l.getBookId(), +1);
            l.setStatus(LoanStatus.CANCELLED);
            return l;
        });
    }

    /** LIBRARIAN issues a reserved book on subscription or to the reading hall. */
    public Loan issue(long loanId, long librarianId, LoanType type) {
        return tx(c -> {
            Loan l = loanDao.findById(loanId).orElseThrow();
            if (l.getStatus() != LoanStatus.ORDERED)
                throw new IllegalStateException("Видати можна лише замовлене");
            LocalDateTime due = type == LoanType.SUBSCRIPTION
                    ? LocalDateTime.now().plusDays(SUBSCRIPTION_DAYS)
                    : LocalDateTime.now().plusHours(READING_HALL_HOURS);
            update(c,
                "UPDATE loans SET status='ISSUED', type=?, librarian_id=?, " +
                "issued_at=now(), due_at=? WHERE id=?",
                ps -> {
                    ps.setString(1, type.name());
                    ps.setLong(2, librarianId);
                    ps.setTimestamp(3, Timestamp.valueOf(due));
                    ps.setLong(4, loanId);
                });
            l.setStatus(LoanStatus.ISSUED);
            l.setType(type);
            l.setLibrarianId(librarianId);
            l.setIssuedAt(LocalDateTime.now());
            l.setDueAt(due);
            log.info("Librarian {} issued loan {} as {}", librarianId, loanId, type);
            return l;
        });
    }

    /** LIBRARIAN accepts a returned book — releases the copy. */
    public Loan returnLoan(long loanId) {
        return tx(c -> {
            Loan l = loanDao.findById(loanId).orElseThrow();
            if (l.getStatus() != LoanStatus.ISSUED)
                throw new IllegalStateException("Повернути можна лише видане");
            update(c, "UPDATE loans SET status='RETURNED', returned_at=now() WHERE id=?",
                    ps -> ps.setLong(1, loanId));
            bookDao.changeAvailable(c, l.getBookId(), +1);
            l.setStatus(LoanStatus.RETURNED);
            l.setReturnedAt(LocalDateTime.now());
            return l;
        });
    }

    // ---------- helpers ----------
    @FunctionalInterface private interface TxBlock<R> { R run(Connection c) throws SQLException; }
    @FunctionalInterface private interface PsConfigurer { void apply(PreparedStatement ps) throws SQLException; }

    private static void update(Connection c, String sql, PsConfigurer cfg) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) { cfg.apply(ps); ps.executeUpdate(); }
    }

    private <R> R tx(TxBlock<R> block) {
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                R r = block.run(c);
                c.commit();
                return r;
            } catch (Exception e) {
                c.rollback();
                if (e instanceof RuntimeException re) throw re;
                throw new RuntimeException(e);
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
