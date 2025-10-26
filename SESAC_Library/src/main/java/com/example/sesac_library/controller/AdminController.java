package com.example.sesac_library.controller;

import com.example.sesac_library.entity.Book;
import com.example.sesac_library.entity.Loan;
import com.example.sesac_library.entity.Reservation;
import com.example.sesac_library.entity.User;
import com.example.sesac_library.repository.BookRepository;
import com.example.sesac_library.repository.LoanRepository;
import com.example.sesac_library.repository.ReservationRepository;
import com.example.sesac_library.repository.UserRepository;
import com.example.sesac_library.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String adminPage(HttpSession session) {
        String username = (String) session.getAttribute("username");

        if (username == null) {
            return "redirect:/login.html";
        }

        Optional<User> userOpt = userService.findByUsername(username);

        if (userOpt.isEmpty() || userOpt.get().getRole() != User.Role.ADMIN) {
            return "redirect:/index.html";
        }

        return "forward:/admin.html";
    }

    /* 대출 현황 조회 */
    @GetMapping("/api/loans")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getActiveLoans(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).build();
        }

        List<Loan> activeLoans = loanRepository.findActiveLoans();
        List<Map<String, Object>> response = activeLoans.stream().map(loan -> {
            Map<String, Object> loanInfo = new HashMap<>();
            loanInfo.put("loanId", loan.getLoanId());
            loanInfo.put("bookId", loan.getBookId());

            Book book = bookRepository.findById(loan.getBookId()).orElse(null);
            if (book != null) {
                loanInfo.put("bookTitle", book.getTitle());
            }

            User user = userRepository.findById(loan.getUserId()).orElse(null);
            if (user != null) {
                loanInfo.put("username", user.getUsername());
            }

            loanInfo.put("loanedAt", loan.getLoanedAt().toString());
            loanInfo.put("dueDate", loan.getDueDate().toString());

            return loanInfo;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /* 강제 반납 */
    @PostMapping("/api/loans/{loanId}/return")
    @ResponseBody
    public ResponseEntity<Map<String, String>> forceReturn(@PathVariable Long loanId, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        }

        Optional<Loan> loanOpt = loanRepository.findById(loanId);
        if (loanOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "대출 정보를 찾을 수 없습니다."));
        }

        Loan loan = loanOpt.get();
        loan.returnBook();
        loanRepository.save(loan);

        return ResponseEntity.ok(Map.of("message", "반납 처리되었습니다."));
    }

    /* 예약 큐 조회 */
    @GetMapping("/api/reservations")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getActiveReservations(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).build();
        }

        List<Reservation> activeReservations = reservationRepository.findActiveReservations();
        List<Map<String, Object>> response = activeReservations.stream().map(reservation -> {
            Map<String, Object> reservationInfo = new HashMap<>();
            reservationInfo.put("reservationId", reservation.getReservationId());
            reservationInfo.put("bookId", reservation.getBookId());

            Book book = bookRepository.findById(reservation.getBookId()).orElse(null);
            if (book != null) {
                reservationInfo.put("bookTitle", book.getTitle());
            }

            User user = userRepository.findById(reservation.getUserId()).orElse(null);
            if (user != null) {
                reservationInfo.put("username", user.getUsername());
            }

            reservationInfo.put("reservationDate", reservation.getReservationDate().toString());

            return reservationInfo;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /* 예약자 대출 처리 */
    @PostMapping("/api/reservations/{reservationId}/loan")
    @ResponseBody
    public ResponseEntity<Map<String, String>> processReservationToLoan(@PathVariable Long reservationId, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        }

        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "예약 정보를 찾을 수 없습니다."));
        }

        Reservation reservation = reservationOpt.get();

        // 새로운 대출 생성
        Loan newLoan = new Loan(reservation.getUserId(), reservation.getBookId());
        loanRepository.save(newLoan);

        // 예약 완료 처리
        reservation.complete();
        reservationRepository.save(reservation);

        return ResponseEntity.ok(Map.of("message", "대출 처리되었습니다."));
    }

    /* [취약점] 사용자 포인트 조작 API - 권한 체크 우회 가능 */
    @PostMapping("/api/users/{userId}/points")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateUserPoints(
            @PathVariable Long userId,
            @RequestBody Map<String, Integer> request,
            HttpSession session) {

        // 취약점 1: 관리자 권한 체크를 주석 처리 (의도적)
        // if (!isAdmin(session)) {
        //     return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        // }

        // 취약점 2: 로그인만 확인 (일반 사용자도 접근 가능)
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        Optional<User> targetUserOpt = userRepository.findById(userId);
        if (targetUserOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "사용자를 찾을 수 없습니다."));
        }

        User targetUser = targetUserOpt.get();
        Integer newPoints = request.get("points");

        if (newPoints == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "포인트 값이 필요합니다."));
        }

        // 취약점 3: 포인트 값 검증 없음 (음수, 과도하게 큰 값 모두 허용)
        targetUser.setPoints(newPoints);
        userRepository.save(targetUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "포인트가 업데이트되었습니다.");
        response.put("userId", userId);
        response.put("newPoints", newPoints);

        return ResponseEntity.ok(response);
    }

    /* 사용자 목록 조회 API */
    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllUsers(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).build();
        }

        List<User> users = userRepository.findAll();
        List<Map<String, Object>> response = users.stream().map(user -> {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getUserId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("points", user.getPoints());
            userInfo.put("role", user.getRole().toString());
            return userInfo;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    private boolean isAdmin(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return false;
        }

        Optional<User> userOpt = userService.findByUsername(username);
        return userOpt.isPresent() && userOpt.get().getRole() == User.Role.ADMIN;
    }
}
