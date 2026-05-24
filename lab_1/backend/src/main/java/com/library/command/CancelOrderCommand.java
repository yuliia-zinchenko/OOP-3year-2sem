package com.library.command;

import com.library.model.Loan;
import com.library.service.LoanService;

public class CancelOrderCommand implements Command<Loan> {
    private final LoanService loanService;
    private final long loanId;
    private final long userId;

    public CancelOrderCommand(LoanService s, long loanId, long userId) {
        this.loanService = s; this.loanId = loanId; this.userId = userId;
    }

    @Override public Loan execute() { return loanService.cancel(loanId, userId); }
}
