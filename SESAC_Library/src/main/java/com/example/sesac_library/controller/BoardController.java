package com.example.sesac_library.controller;

import com.example.sesac_library.dto.BoardResponse;
import com.example.sesac_library.entity.Board;
import com.example.sesac_library.entity.User;
import com.example.sesac_library.repository.UserRepository;
import com.example.sesac_library.service.BoardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

@RestController
@RequestMapping("/api/board")
public class BoardController {

    private final BoardService boardService;
    private final UserRepository userRepository;

    @Autowired
    private javax.sql.DataSource dataSource;

    public BoardController(BoardService boardService, UserRepository userRepository) {
        this.boardService = boardService;
        this.userRepository = userRepository;
    }

    // 게시글 작성 (파일 업로드 포함 + 100포인트 적립)
    @PostMapping("/write")
    @Transactional
    public ResponseEntity<?> createBoard(
            @RequestParam("userId") Long userId,
            @RequestParam("category") String category,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("password_hash") String passwordHash,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpSession session) throws IOException {

        Map<String, Object> response = new HashMap<>();

        try {
            // 세션 확인
            String sessionUsername = (String) session.getAttribute("username");
            if (sessionUsername == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(response);
            }

            // 사용자 확인
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "사용자를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            User user = userOpt.get();

            // 게시글 저장
            Board board = new Board();
            board.setUserId(userId);
            board.setCategory(category);
            board.setTitle(title);
            board.setContent(content);
            board.setPasswordHash(passwordHash);

            Board savedBoard = boardService.createBoard(board, file);

            // 게시글 작성 시 100포인트 지급
            user.setPoints(user.getPoints() + 10);
            userRepository.save(user);


            BoardResponse boardResponse = boardService.findByIdWithUsername(savedBoard.getBoardId());

            response.put("success", true);
            response.put("message", "게시글이 작성되었습니다. (+10포인트)");
            response.put("board", boardResponse);
            response.put("currentPoints", user.getPoints());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "게시글 작성 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // 게시글 페이징 조회 (username 포함)
    @GetMapping("/list")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAllBoardsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("boardId").descending());

        Page<BoardResponse> boardPage;
        if (category != null && !category.isEmpty() && !category.equals("all")) {
            boardPage = boardService.findByCategoryWithUsername(category, pageable);
        } else {
            boardPage = boardService.findAllWithUsername(pageable);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("boards", boardPage.getContent());
        response.put("currentPage", boardPage.getNumber());
        response.put("totalPages", boardPage.getTotalPages());
        response.put("totalItems", boardPage.getTotalElements());
        response.put("hasNext", boardPage.hasNext());
        response.put("hasPrevious", boardPage.hasPrevious());

        return ResponseEntity.ok(response);
    }

    // 게시글 단건 조회 (username 포함)
    @GetMapping("/list/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<BoardResponse> getBoardById(@PathVariable Long id) {
        BoardResponse response = boardService.findByIdWithUsername(id);
        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 게시글 수정
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateBoard(
            @PathVariable Long id,
            @RequestParam("userId") Long userId,
            @RequestParam("category") String category,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpSession session) throws IOException {

        String sessionUsername = (String) session.getAttribute("username");
        if (sessionUsername == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        Board existingBoard = boardService.findByIdWithFiles(id);
        if (existingBoard == null) {
            return ResponseEntity.notFound().build();
        }

        if (!existingBoard.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body("본인이 작성한 게시글만 수정할 수 있습니다.");
        }

        existingBoard.setCategory(category);
        existingBoard.setTitle(title);
        existingBoard.setContent(content);

        Board updatedBoard = boardService.updateBoard(existingBoard, file);
        BoardResponse response = boardService.findByIdWithUsername(updatedBoard.getBoardId());
        return ResponseEntity.ok(response);
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteBoard(
            @PathVariable Long id,
            @RequestParam("userId") Long userId,
            HttpSession session) {

        String sessionUsername = (String) session.getAttribute("username");
        if (sessionUsername == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        Board existingBoard = boardService.findByIdWithFiles(id);
        if (existingBoard == null) {
            return ResponseEntity.notFound().build();
        }

        if (!existingBoard.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body("본인이 작성한 게시글만 삭제할 수 있습니다.");
        }

        boardService.deleteBoard(id);
        return ResponseEntity.ok().body("게시글이 삭제되었습니다.");
    }

    /* [취약점] SQL Injection - 게시글 검색 기능 */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchBoards(@RequestParam String keyword) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 🚨 취약점: 사용자 입력을 직접 SQL에 삽입 (SQL Injection 가능)
            String sql = "SELECT b.board_id, b.title, b.content, b.category, b.created_at, " +
                        "u.username, u.email, u.phone, u.addr " +
                        "FROM board b " +
                        "LEFT JOIN users u ON b.user_id = u.user_id " +
                        "WHERE b.title LIKE '%" + keyword + "%' " +
                        "OR b.content LIKE '%" + keyword + "%' " +
                        "ORDER BY b.board_id DESC";

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                Map<String, Object> board = new HashMap<>();
                board.put("boardId", rs.getLong("board_id"));
                board.put("title", rs.getString("title"));
                board.put("content", rs.getString("content"));
                board.put("category", rs.getString("category"));
                board.put("createdAt", rs.getTimestamp("created_at"));
                board.put("username", rs.getString("username"));

                // 취약점: 민감한 개인정보까지 노출
                board.put("email", rs.getString("email"));
                board.put("phone", rs.getString("phone"));
                board.put("addr", rs.getString("addr"));

                results.add(board);
            }

            response.put("success", true);
            response.put("keyword", keyword);
            response.put("results", results);
            response.put("count", results.size());

        } catch (Exception e) {
            // 취약점: 상세한 에러 메시지 노출 (SQL 구조 유출)
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getName());
            response.put("sql_hint", "Check your SQL syntax near: " + keyword);
        }

        return ResponseEntity.ok(response);
    }
}