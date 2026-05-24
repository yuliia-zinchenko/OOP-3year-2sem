package com.library.command;

import com.library.model.Loan;
import com.library.model.LoanType;
import com.library.service.LoanService;

/** Librarian issues an ordered book on subscription or to the reading hall. */
public class IssueBookCommand implements Command<Loan> {
    private final LoanService loanService;
    private final long loanId;
    private final long librarianId;
    private final LoanType type;

    public IssueBookCommand(LoanService s, long loanId, long librarianId, LoanType type) {
        this.loanService = s; this.loanId = loanId;
        this.librarianId = librarianId; this.type = type;
    }

    @Override public Loan execute() { return loanService.issue(loanId, librarianId, type); }
}
