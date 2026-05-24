package com.library.repository;

import com.library.domain.Loan;
import com.library.domain.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserIdOrderByOrderedAtDesc(Long userId);
    List<Loan> findByStatusOrderByOrderedAtAsc(LoanStatus status);
}
