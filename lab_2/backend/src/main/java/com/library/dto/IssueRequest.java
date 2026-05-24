package com.library.dto;

import com.library.domain.LoanType;
import jakarta.validation.constraints.NotNull;

public record IssueRequest(@NotNull LoanType type) {}
