package com.library.dto;

import com.library.model.LoanStatus;
import com.library.model.LoanType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDto {
    private Long id;
    private Long userId;
    private Long bookId;
    private String bookTitle;
    private String readerEmail;
    private Long librarianId;
    private LoanStatus status;
    private LoanType type;
    private LocalDateTime orderedAt;
    private LocalDateTime issuedAt;
    private LocalDateTime dueAt;
    private LocalDateTime returnedAt;
}
