package com.library.command;

import com.library.model.Loan;
import com.library.service.LoanService;

/** Reader places an order for a book. */
public class OrderBookCommand implements Command<Loan> {
    private final LoanService loanService;
    private final long userId;
    private final long bookId;

    public OrderBookCommand(LoanService s, long userId, long bookId) {
        this.loanService = s; this.userId = userId; this.bookId = bookId;
    }

    @Override public Loan execute() { return loanService.order(userId, bookId); }
}
