package com.example.sesac_library.repository;

import com.example.sesac_library.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserIdAndStatus(Long userId, String status);

    List<Loan> findByBookIdAndStatus(Long bookId, String status);

    Optional<Loan> findByUserIdAndBookIdAndStatus(Long userId, Long bookId, String status);

    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE'")
    List<Loan> findActiveLoans();

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.bookId = :bookId AND l.status = 'ACTIVE'")
    Long countActiveLoansByBookId(@Param("bookId") Long bookId);

    @Query("SELECT l FROM Loan l WHERE l.userId = :userId AND l.status = 'ACTIVE'")
    List<Loan> findActiveLoansByUserId(@Param("userId") Long userId);
}