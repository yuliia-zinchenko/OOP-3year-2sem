package com.library.dto;

import jakarta.validation.constraints.NotNull;

public record OrderRequest(@NotNull Long bookId) {}
