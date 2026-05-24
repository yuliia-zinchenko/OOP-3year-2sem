package com.library.command;

import com.library.model.Loan;
import com.library.service.LoanService;

public class ReturnBookCommand implements Command<Loan> {
    private final LoanService loanService;
    private final long loanId;

    public ReturnBookCommand(LoanService s, long loanId) {
        this.loanService = s; this.loanId = loanId;
    }

    @Override public Loan execute() { return loanService.returnLoan(loanId); }
}
