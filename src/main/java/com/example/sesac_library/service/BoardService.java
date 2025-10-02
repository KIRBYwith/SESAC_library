package com.example.sesac_library.service;

import com.example.sesac_library.dto.BoardResponse;
import com.example.sesac_library.entity.Board;
import com.example.sesac_library.entity.BoardFile;
import com.example.sesac_library.entity.User;
import com.example.sesac_library.repository.BoardFileRepository;
import com.example.sesac_library.repository.BoardRepository;
import com.example.sesac_library.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardFileRepository boardFileRepository;
    private final UserRepository userRepository;

    public BoardService(BoardRepository boardRepository,
                        BoardFileRepository boardFileRepository,
                        UserRepository userRepository) {
        this.boardRepository = boardRepository;
        this.boardFileRepository = boardFileRepository;
        this.userRepository = userRepository;
    }

    // 게시글 저장 (파일 업로드 포함)
    @Transactional
    public Board createBoard(Board board, MultipartFile file) throws IOException {
        Board savedBoard = boardRepository.save(board);

        if (file != null && !file.isEmpty()) {
            String uploadDir = "C://upload/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String savedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String filePath = uploadDir + savedFileName;
            file.transferTo(new File(filePath));

            BoardFile boardFile = new BoardFile();
            boardFile.setBoard(savedBoard);
            boardFile.setFileName(file.getOriginalFilename());
            boardFile.setFileUrl("/files/" + savedFileName);
            boardFileRepository.save(boardFile);
        }

        return savedBoard;
    }

    // ✅ 게시글 + 작성자 이름 조회 (단건)
    @Transactional
    public BoardResponse findByIdWithUsername(Long boardId) {
        Optional<Board> boardOpt = boardRepository.findById(boardId);
        if (boardOpt.isPresent()) {
            Board board = boardOpt.get();

            // 조회수 증가
            board.setViews(board.getViews() + 1);
            boardRepository.save(board);

            // 파일 정보 조회
            List<BoardFile> files = boardFileRepository.findByBoard_BoardId(boardId);
            board.setFiles(files);

            // 작성자 이름 조회
            String userName = getUserNameById(board.getUserId());

            return BoardResponse.from(board, userName);
        }
        return null;
    }

    // ✅ 전체 게시글 + 작성자 이름 조회 (페이징)
    @Transactional(readOnly = true)
    public Page<BoardResponse> findAllWithUsername(Pageable pageable) {
        Page<Board> boardPage = boardRepository.findAll(pageable);
        return boardPage.map(board -> {
            String userName = getUserNameById(board.getUserId());
            return BoardResponse.from(board, userName);
        });
    }

    // ✅ 카테고리별 게시글 + 작성자 이름 조회 (페이징)
    @Transactional(readOnly = true)
    public Page<BoardResponse> findByCategoryWithUsername(String category, Pageable pageable) {
        Page<Board> boardPage = boardRepository.findByCategory(category, pageable);
        return boardPage.map(board -> {
            String userName = getUserNameById(board.getUserId());
            return BoardResponse.from(board, userName);
        });
    }

    // 파일 정보 포함 조회 (수정/삭제용)
    @Transactional(readOnly = true)
    public Board findByIdWithFiles(Long boardId) {
        Optional<Board> boardOpt = boardRepository.findById(boardId);
        if (boardOpt.isPresent()) {
            Board board = boardOpt.get();
            List<BoardFile> files = boardFileRepository.findByBoard_BoardId(boardId);
            board.setFiles(files);
            return board;
        }
        return null;
    }

    // 게시글 수정
    @Transactional
    public Board updateBoard(Board board, MultipartFile file) throws IOException {
        Board updatedBoard = boardRepository.save(board);

        if (file != null && !file.isEmpty()) {
            String uploadDir = "C://upload/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String savedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String filePath = uploadDir + savedFileName;
            file.transferTo(new File(filePath));

            BoardFile boardFile = new BoardFile();
            boardFile.setBoard(updatedBoard);
            boardFile.setFileName(file.getOriginalFilename());
            boardFile.setFileUrl("/files/" + savedFileName);
            boardFileRepository.save(boardFile);
        }

        return updatedBoard;
    }

    // 게시글 삭제
    @Transactional
    public void deleteBoard(Long boardId) {
        List<BoardFile> files = boardFileRepository.findByBoard_BoardId(boardId);

        for (BoardFile file : files) {
            try {
                String fileName = file.getFileUrl().replace("/files/", "");
                File physicalFile = new File("C://upload/" + fileName);
                if (physicalFile.exists()) {
                    physicalFile.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        boardRepository.deleteById(boardId);
    }

    // ✅ 작성자 이름 조회 헬퍼 메서드
    private String getUserNameById(Long userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse("알 수 없음");
    }
}