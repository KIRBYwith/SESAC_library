package com.example.sesac_library.controller;

import com.example.sesac_library.dto.BoardResponse;
import com.example.sesac_library.entity.Board;
import com.example.sesac_library.service.BoardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/board")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    // 게시글 작성 (파일 업로드 포함)
    @PostMapping("/write")
    @Transactional
    public ResponseEntity<BoardResponse> createBoard(
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
        return ResponseEntity.ok(BoardResponse.from(savedBoard));
    }

    // 게시글 페이징 조회 (새로 추가)
    @GetMapping("/list")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAllBoardsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {

        // 최신글이 먼저 나오도록 boardId 기준 내림차순 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by("boardId").descending());

        Page<Board> boardPage;
        if (category != null && !category.isEmpty() && !category.equals("all")) {
            boardPage = boardService.findByCategory(category, pageable);
        } else {
            boardPage = boardService.findAll(pageable);
        }

        List<BoardResponse> boards = boardPage.getContent().stream()
                .map(BoardResponse::from)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("boards", boards);
        response.put("currentPage", boardPage.getNumber());
        response.put("totalPages", boardPage.getTotalPages());
        response.put("totalItems", boardPage.getTotalElements());
        response.put("hasNext", boardPage.hasNext());
        response.put("hasPrevious", boardPage.hasPrevious());

        return ResponseEntity.ok(response);
    }

    // 게시글 단건 조회 (파일까지 포함)
    @GetMapping("/list/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<BoardResponse> getBoardById(@PathVariable Long id) {
        Board board = boardService.findByIdWithFiles(id);
        if (board != null) {
            return ResponseEntity.ok(BoardResponse.from(board));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 게시글 수정
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateBoard(
            @PathVariable Long id,
            @RequestParam("userId") Integer userId,
            @RequestParam("category") String category,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        // 게시글 존재 여부 확인
        Board existingBoard = boardService.findByIdWithFiles(id);
        if (existingBoard == null) {
            return ResponseEntity.notFound().build();
        }

        // 작성자 본인 확인
        if (!existingBoard.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body("본인이 작성한 게시글만 수정할 수 있습니다.");
        }

        // 게시글 수정
        existingBoard.setCategory(category);
        existingBoard.setTitle(title);
        existingBoard.setContent(content);

        Board updatedBoard = boardService.updateBoard(existingBoard, file);
        return ResponseEntity.ok(BoardResponse.from(updatedBoard));
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteBoard(
            @PathVariable Long id,
            @RequestParam("userId") Integer userId) {

        // 게시글 존재 여부 확인
        Board existingBoard = boardService.findByIdWithFiles(id);
        if (existingBoard == null) {
            return ResponseEntity.notFound().build();
        }

        // 작성자 본인 확인
        if (!existingBoard.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body("본인이 작성한 게시글만 삭제할 수 있습니다.");
        }

        // 게시글 삭제 (연관된 파일도 함께 삭제됨 - CASCADE)
        boardService.deleteBoard(id);
        return ResponseEntity.ok().body("게시글이 삭제되었습니다.");
    }
}