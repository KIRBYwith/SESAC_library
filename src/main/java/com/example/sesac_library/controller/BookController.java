package com.example.sesac_library.controller;

import com.example.sesac_library.entity.Book;
import com.example.sesac_library.entity.Loan;
import com.example.sesac_library.entity.Reservation;
import com.example.sesac_library.entity.User;
import com.example.sesac_library.repository.BookRepository;
import com.example.sesac_library.repository.LoanRepository;
import com.example.sesac_library.repository.ReservationRepository;
import com.example.sesac_library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        List<Book> books = bookRepository.findAll();
        return ResponseEntity.ok(books);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        Optional<Book> book = bookRepository.findById(id);
        return book.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        List<Book> books;
        if (keyword != null && !keyword.trim().isEmpty()) {
            books = bookRepository.searchBooks(keyword, categoryId);
        } else if (categoryId != null) {
            books = bookRepository.findByCategoryId(categoryId);
        } else {
            books = bookRepository.findAll();
        }
        return ResponseEntity.ok(books);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Book>> getAvailableBooks() {
        List<Book> books = bookRepository.findAvailableBooks();
        return ResponseEntity.ok(books);
    }

    @PostMapping("/{bookId}/borrow")
    public ResponseEntity<Map<String, Object>> borrowBook(
            @PathVariable Long bookId,
            @RequestParam Long userId) {

        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Book> bookOpt = bookRepository.findById(bookId);
            Optional<User> userOpt = userRepository.findById(userId);

            if (!bookOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "책을 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "사용자를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            Book book = bookOpt.get();
            User user = userOpt.get();

            if (book.getBookCopies() <= 0) {
                response.put("success", false);
                response.put("message", "대출 가능한 책이 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            Optional<Loan> existingLoan = loanRepository.findByUserIdAndBookIdAndStatus(userId, bookId, "ACTIVE");
            if (existingLoan.isPresent()) {
                response.put("success", false);
                response.put("message", "이미 대출 중인 책입니다.");
                return ResponseEntity.badRequest().body(response);
            }

            Loan loan = new Loan(userId, bookId);
            loanRepository.save(loan);

            book.setBookCopies(book.getBookCopies() - 1);
            bookRepository.save(book);

            response.put("success", true);
            response.put("message", "대출이 완료되었습니다.");
            response.put("dueDate", loan.getDueDate());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "대출 처리 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/{bookId}/reserve")
    public ResponseEntity<Map<String, Object>> reserveBook(
            @PathVariable Long bookId,
            @RequestParam Long userId) {

        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Book> bookOpt = bookRepository.findById(bookId);
            Optional<User> userOpt = userRepository.findById(userId);

            if (!bookOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "책을 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "사용자를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            // 이미 대출 중인지 확인
            Optional<Loan> existingLoan = loanRepository.findByUserIdAndBookIdAndStatus(userId, bookId, "ACTIVE");
            if (existingLoan.isPresent()) {
                response.put("success", false);
                response.put("message", "이미 대출 중입니다.");
                return ResponseEntity.badRequest().body(response);
            }

            // 이미 예약한 책인지 확인
            Optional<Reservation> existingReservation = reservationRepository.findByUserIdAndBookIdAndStatus(userId, bookId, "ACTIVE");
            if (existingReservation.isPresent()) {
                response.put("success", false);
                response.put("message", "이미 예약한 책입니다.");
                return ResponseEntity.badRequest().body(response);
            }

            Reservation reservation = new Reservation(userId, bookId);
            reservationRepository.save(reservation);

            Long reservationCount = reservationRepository.countActiveReservationsByBookId(bookId);

            response.put("success", true);
            response.put("message", "예약이 완료되었습니다.");
            response.put("reservationOrder", reservationCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "예약 처리 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/{bookId}/status")
    public ResponseEntity<Map<String, Object>> getBookStatus(@PathVariable Long bookId) {
        Map<String, Object> response = new HashMap<>();

        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (!bookOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "책을 찾을 수 없습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        Book book = bookOpt.get();
        Long activeLoanCount = loanRepository.countActiveLoansByBookId(bookId);
        Long reservationCount = reservationRepository.countActiveReservationsByBookId(bookId);

        response.put("success", true);
        response.put("bookCopies", book.getBookCopies());
        response.put("activeLoanCount", activeLoanCount);
        response.put("reservationCount", reservationCount);
        response.put("isAvailable", book.getBookCopies() > 0);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getBookStats() {
        Map<String, Object> stats = new HashMap<>();

        List<Book> allBooks = bookRepository.findAll();
        List<Book> availableBooks = bookRepository.findAvailableBooks();
        List<Book> borrowedBooks = bookRepository.findBorrowedBooks();

        stats.put("totalBooks", allBooks.size());
        stats.put("availableBooks", availableBooks.size());
        stats.put("borrowedBooks", borrowedBooks.size());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/user/{userId}/loans")
    public ResponseEntity<List<Map<String, Object>>> getUserLoans(@PathVariable Long userId) {
        try {
            List<Loan> userLoans = loanRepository.findActiveLoansByUserId(userId);
            List<Map<String, Object>> loanDetails = new ArrayList<>();

            for (Loan loan : userLoans) {
                Optional<Book> bookOpt = bookRepository.findById(loan.getBookId());
                if (bookOpt.isPresent()) {
                    Book book = bookOpt.get();
                    Map<String, Object> loanInfo = new HashMap<>();
                    loanInfo.put("loanId", loan.getLoanId());
                    loanInfo.put("bookId", book.getBookId());
                    loanInfo.put("title", book.getTitle());
                    loanInfo.put("author", book.getAuthor());
                    loanInfo.put("publisher", book.getPublisher());
                    loanInfo.put("loanedAt", loan.getLoanedAt());
                    loanInfo.put("dueDate", loan.getDueDate());
                    loanInfo.put("status", loan.getStatus());
                    loanDetails.add(loanInfo);
                }
            }

            return ResponseEntity.ok(loanDetails);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}