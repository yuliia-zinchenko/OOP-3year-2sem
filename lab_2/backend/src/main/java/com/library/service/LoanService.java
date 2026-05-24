package com.library.service;

import com.library.domain.*;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loans;
    private final BookRepository books;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<Loan> myLoans() {
        return loans.findByUserIdOrderByOrderedAtDesc(currentUser.current().getId());
    }

    @Transactional(readOnly = true)
    public List<Loan> byStatus(LoanStatus status) {
        return loans.findByStatusOrderByOrderedAtAsc(status);
    }

    @Transactional
    public Loan order(Long bookId) {
        UserAccount me = currentUser.current();
        Book book = books.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "book not found"));
        if (book.getAvailableCopies() <= 0)
            throw new ResponseStatusException(CONFLICT, "no copies available");
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        Loan loan = Loan.builder()
                .user(me).book(book)
                .status(LoanStatus.ORDERED)
                .orderedAt(LocalDateTime.now())
                .build();
        return loans.save(loan);
    }

    @Transactional
    public Loan cancel(Long loanId) {
        Loan loan = mustOwn(loanId);
        if (loan.getStatus() != LoanStatus.ORDERED)
            throw new ResponseStatusException(CONFLICT, "only ORDERED can be cancelled");
        loan.setStatus(LoanStatus.CANCELLED);
        Book b = loan.getBook();
        b.setAvailableCopies(b.getAvailableCopies() + 1);
        return loan;
    }

    @Transactional
    public Loan issue(Long loanId, LoanType type) {
        UserAccount librarian = currentUser.current();
        if (librarian.getRole() != Role.LIBRARIAN)
            throw new ResponseStatusException(FORBIDDEN, "librarian only");
        Loan loan = loans.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "loan not found"));
        if (loan.getStatus() != LoanStatus.ORDERED)
            throw new ResponseStatusException(CONFLICT, "only ORDERED can be issued");
        loan.setStatus(LoanStatus.ISSUED);
        loan.setType(type);
        loan.setLibrarian(librarian);
        loan.setIssuedAt(LocalDateTime.now());
        if (type == LoanType.SUBSCRIPTION) {
            loan.setDueAt(LocalDateTime.now().plusDays(30));
        }
        return loan;
    }

    @Transactional
    public Loan acceptReturn(Long loanId) {
        UserAccount librarian = currentUser.current();
        if (librarian.getRole() != Role.LIBRARIAN)
            throw new ResponseStatusException(FORBIDDEN, "librarian only");
        Loan loan = loans.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "loan not found"));
        if (loan.getStatus() != LoanStatus.ISSUED)
            throw new ResponseStatusException(CONFLICT, "only ISSUED can be returned");
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnedAt(LocalDateTime.now());
        Book b = loan.getBook();
        b.setAvailableCopies(b.getAvailableCopies() + 1);
        return loan;
    }

    private Loan mustOwn(Long loanId) {
        UserAccount me = currentUser.current();
        Loan loan = loans.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "loan not found"));
        if (!loan.getUser().getId().equals(me.getId()) && me.getRole() != Role.LIBRARIAN)
            throw new ResponseStatusException(FORBIDDEN, "not your loan");
        return loan;
    }
}
