package com.example.sesac_library.controller;

import com.example.sesac_library.dto.BoardRequest;
import com.example.sesac_library.dto.BoardResponse;
import com.example.sesac_library.entity.Board;
import com.example.sesac_library.service.BoardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/board")
public class BoardController {

    private final BoardService boardService;
    private final ObjectMapper objectMapper;

    public BoardController(BoardService boardService, ObjectMapper objectMapper) {
        this.boardService = boardService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Controller is working!");
    }

    @PostMapping("/write")
    public ResponseEntity<?> writeBoard(
            @RequestParam("userId") Integer userId,
            @RequestParam("category") String category,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("passwordHash") String passwordHash,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            // BoardRequest 생성
            BoardRequest boardRequest = new BoardRequest();
            boardRequest.setUserId(userId);
            boardRequest.setCategory(category);
            boardRequest.setTitle(title);
            boardRequest.setContent(content);
            boardRequest.setPasswordHash(passwordHash);

            // DTO를 Entity로 변환
            Board board = convertToEntity(boardRequest);

            Board saved = boardService.save(board, file);

            // BoardResponse로 변환해서 응답
            return ResponseEntity.ok(BoardResponse.from(saved));
        } catch (Exception e) {
            e.printStackTrace(); // 디버깅용 로그
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getBoardList() {
        try {
            return ResponseEntity.ok(boardService.findAllByOrderByCreatedAtDesc());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBoard(@PathVariable Long id) {
        try {
            Board board = boardService.findByIdAndIncrementViews(id);
            return ResponseEntity.ok(BoardResponse.from(board));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    private Board convertToEntity(BoardRequest request) {
        Board board = new Board();
        board.setUserId(request.getUserId());
        board.setCategory(request.getCategory());
        board.setTitle(request.getTitle());
        board.setContent(request.getContent());
        board.setPasswordHash(request.getPasswordHash());
        return board;
    }
}