package com.library.controller;

import com.library.domain.LoanStatus;
import com.library.dto.IssueRequest;
import com.library.dto.LoanDto;
import com.library.dto.OrderRequest;
import com.library.mapper.LoanMapper;
import com.library.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loans;
    private final LoanMapper mapper;

    @GetMapping
    public List<LoanDto> list(@RequestParam(value = "status", required = false) LoanStatus status) {
        if (status == LoanStatus.ORDERED) {
            return mapper.toDto(loans.byStatus(LoanStatus.ORDERED));
        }
        return mapper.toDto(loans.myLoans());
    }

    @PostMapping
    public LoanDto order(@Valid @RequestBody OrderRequest req) {
        return mapper.toDto(loans.order(req.bookId()));
    }

    @PostMapping("/{id}/cancel")
    public LoanDto cancel(@PathVariable Long id) {
        return mapper.toDto(loans.cancel(id));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public LoanDto issue(@PathVariable Long id, @Valid @RequestBody IssueRequest req) {
        return mapper.toDto(loans.issue(id, req.type()));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public LoanDto acceptReturn(@PathVariable Long id) {
        return mapper.toDto(loans.acceptReturn(id));
    }
}
