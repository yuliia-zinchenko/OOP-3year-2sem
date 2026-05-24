package com.library.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {
    private Long id;
    private Long userId;
    private Long bookId;
    private Long librarianId;
    private LoanStatus status;
    private LoanType type;
    private LocalDateTime orderedAt;
    private LocalDateTime issuedAt;
    private LocalDateTime dueAt;
    private LocalDateTime returnedAt;
}
