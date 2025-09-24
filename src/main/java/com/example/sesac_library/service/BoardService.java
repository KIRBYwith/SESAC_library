package com.example.sesac_library.service;

import com.example.sesac_library.entity.Board;
import com.example.sesac_library.entity.BoardFile;
import com.example.sesac_library.repository.BoardFileRepository;
import com.example.sesac_library.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BoardService {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardFileRepository boardFileRepository;

    private final String uploadDir = "C:/upload/";

    public Board save(Board board, MultipartFile file) throws Exception {
        // 업로드 디렉토리 생성
        createUploadDirectory();

        // 게시글 저장
        Board savedBoard = boardRepository.save(board);

        // 파일이 있으면 저장
        if (file != null && !file.isEmpty()) {
            String savedFileName = saveFile(file);

            BoardFile boardFile = new BoardFile();
            boardFile.setBoard(savedBoard);
            boardFile.setFileName(file.getOriginalFilename());
            boardFile.setFileUrl("/files/" + savedFileName);
            boardFileRepository.save(boardFile);
        }

        // 파일 정보와 함께 다시 조회하여 반환
        return findByIdWithFiles(savedBoard.getBoardId());
    }

    @Transactional(readOnly = true)
    public List<Board> findAllByOrderByCreatedAtDesc() {
        return boardRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Board findByIdAndIncrementViews(Long id) {
        Board board = findByIdWithFiles(id);

        // 조회수 증가
        boardRepository.incrementViews(id);

        return board;
    }

    @Transactional(readOnly = true)
    public List<Board> findByCategoryOrderByCreatedAtDesc(String category) {
        return boardRepository.findByCategoryOrderByCreatedAtDesc(category);
    }

    // 파일 정보와 함께 게시글 조회
    @Transactional(readOnly = true)
    public Board findByIdWithFiles(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 파일 정보 명시적으로 로드
        List<BoardFile> files = boardFileRepository.findByBoard_BoardId(id);
        board.setFiles(files);

        return board;
    }

    private void createUploadDirectory() throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    private String saveFile(MultipartFile file) throws IOException {
        // 파일명 중복 방지를 위해 UUID 사용
        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String savedFileName = UUID.randomUUID().toString() + extension;

        Path filePath = Paths.get(uploadDir + savedFileName);
        Files.write(filePath, file.getBytes());

        return savedFileName;
    }
}