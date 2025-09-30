package com.example.sesac_library.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan")
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "loaned_at", nullable = false)
    private LocalDateTime loanedAt;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @PrePersist
    protected void onCreate() {
        if (loanedAt == null) {
            loanedAt = LocalDateTime.now();
        }
        if (dueDate == null) {
            dueDate = loanedAt.toLocalDate().plusDays(7); // 7일 대출
        }
    }

    public Loan() {}

    public Loan(Long userId, Long bookId) {
        this.userId = userId;
        this.bookId = bookId;
        this.loanedAt = LocalDateTime.now();
        this.dueDate = this.loanedAt.toLocalDate().plusDays(7);
        this.status = "ACTIVE";
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public LocalDateTime getLoanedAt() {
        return loanedAt;
    }

    public void setLoanedAt(LocalDateTime loanedAt) {
        this.loanedAt = loanedAt;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void returnBook() {
        this.returnedAt = LocalDateTime.now();
        this.status = "RETURNED";
    }
}