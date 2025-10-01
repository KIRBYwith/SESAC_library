package com.example.sesac_library.controller;

import com.example.sesac_library.dto.BoardResponse;
import com.example.sesac_library.entity.Board;
import com.example.sesac_library.entity.User;  // ✅ 추가
import com.example.sesac_library.repository.UserRepository;  // ✅ 추가
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
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/board")
public class BoardController {

    private final BoardService boardService;
    private final UserRepository userRepository;  // ✅ 추가

    // ✅ 생성자에 UserRepository 추가
    public BoardController(BoardService boardService, UserRepository userRepository) {
        this.boardService = boardService;
        this.userRepository = userRepository;
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

        // ✅ userName 조회해서 함께 전달
        String userName = userRepository.findByUserId(userId)
                .map(User::getUsername)
                .orElse("알 수 없음");

        return ResponseEntity.ok(BoardResponse.from(savedBoard, userName));
    }

    // 게시글 페이징 조회
    @GetMapping("/list")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAllBoardsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("boardId").descending());

        Page<Board> boardPage;
        if (category != null && !category.isEmpty() && !category.equals("all")) {
            boardPage = boardService.findByCategory(category, pageable);
        } else {
            boardPage = boardService.findAll(pageable);
        }

        // ✅ 각 게시글마다 userName 조회
        List<BoardResponse> boards = boardPage.getContent().stream()
                .map(board -> {
                    String userName = userRepository.findByUserId(board.getUserId())
                            .map(User::getUsername)
                            .orElse("알 수 없음");
                    return BoardResponse.from(board, userName);
                })
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
        // ✅ findByIdWithFiles는 이미 BoardResponse 반환
        BoardResponse boardResponse = boardService.findByIdWithFiles(id);
        if (boardResponse != null) {
            return ResponseEntity.ok(boardResponse);
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

        // ✅ findByIdWithFiles는 BoardResponse 반환
        BoardResponse existingBoardResponse = boardService.findByIdWithFiles(id);
        if (existingBoardResponse == null) {
            return ResponseEntity.notFound().build();
        }

        // 작성자 본인 확인
        if (!existingBoardResponse.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body("본인이 작성한 게시글만 수정할 수 있습니다.");
        }

        // ✅ Board 엔티티 다시 조회 (수정을 위해)
        Board existingBoard = new Board();
        existingBoard.setBoardId(id);
        existingBoard.setUserId(userId);
        existingBoard.setCategory(category);
        existingBoard.setTitle(title);
        existingBoard.setContent(content);

        Board updatedBoard = boardService.updateBoard(existingBoard, file);

        // ✅ userName 조회
        String userName = userRepository.findByUserId(userId)
                .map(User::getUsername)
                .orElse("알 수 없음");

        return ResponseEntity.ok(BoardResponse.from(updatedBoard, userName));
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteBoard(
            @PathVariable Long id,
            @RequestParam("userId") Integer userId) {

        // ✅ findByIdWithFiles는 BoardResponse 반환
        BoardResponse existingBoardResponse = boardService.findByIdWithFiles(id);
        if (existingBoardResponse == null) {
            return ResponseEntity.notFound().build();
        }

        // 작성자 본인 확인
        if (!existingBoardResponse.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body("본인이 작성한 게시글만 삭제할 수 있습니다.");
        }

        boardService.deleteBoard(id);
        return ResponseEntity.ok().body("게시글이 삭제되었습니다.");
    }
}