package com.example.sesac_library.controller;

import com.example.sesac_library.entity.Board;
import com.example.sesac_library.service.BoardService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/board")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    // 게시글 작성 (파일 업로드 포함) - 개별 파라미터로 수정
    @PostMapping("/write")
    @Transactional
    public ResponseEntity<Board> createBoard(
            @RequestParam("userId") Integer userId,
            @RequestParam("category") String category,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("password_hash") String passwordHash,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        Board board = new Board();
        board.setUserId(userId);
        board.setCategory(category);
        board.setTitle(title);
        board.setContent(content);
        board.setPasswordHash(passwordHash);

        Board savedBoard = boardService.createBoard(board, file);
        return ResponseEntity.ok(savedBoard);
    }

    // 게시글 전체 조회
    @GetMapping("/list")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Board>> getAllBoards() {
        return ResponseEntity.ok(boardService.findAll());
    }

    // 게시글 단건 조회 (파일까지 포함)
    @GetMapping("/list/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Board> getBoardById(@PathVariable Long id) {
        Board board = boardService.findByIdWithFiles(id);
        if (board != null) {
            return ResponseEntity.ok(board);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}