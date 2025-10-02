package com.example.sesac_library.service;

import com.example.sesac_library.entity.Board;
import com.example.sesac_library.entity.BoardFile;
import com.example.sesac_library.repository.BoardFileRepository;
import com.example.sesac_library.repository.BoardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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

    public BoardService(BoardRepository boardRepository, BoardFileRepository boardFileRepository) {
        this.boardRepository = boardRepository;
        this.boardFileRepository = boardFileRepository;
    }

    // 게시글 저장 (파일 업로드 포함)
    public Board createBoard(Board board, MultipartFile file) throws IOException {
        // 먼저 게시글 저장
        Board savedBoard = boardRepository.save(board);

        // 파일이 있으면 저장 처리
        if (file != null && !file.isEmpty()) {
            // 업로드 경로 (로컬 C드라이브에 저장)
            String uploadDir = "C://upload/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs(); // 경로 없으면 생성
            }

            // 파일명 충돌 방지를 위한 UUID 붙이기
            String savedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String filePath = uploadDir + savedFileName;

            // 실제 파일 저장
            file.transferTo(new File(filePath));

            // DB에 파일 정보 저장
            BoardFile boardFile = new BoardFile();
            boardFile.setBoard(savedBoard);
            boardFile.setFileName(file.getOriginalFilename()); // 원본 파일명
            boardFile.setFileUrl("/files/" + savedFileName);  // 접근 경로 (WebMvcConfig 필요)
            boardFileRepository.save(boardFile);
        }

        // 파일까지 포함해서 다시 조회 후 반환
        return findByIdWithFiles(savedBoard.getBoardId());
    }

    // 특정 게시글 + 파일 정보까지 조회
    public Board findByIdWithFiles(Long boardId) {
        Optional<Board> boardOpt = boardRepository.findById(boardId);
        if (boardOpt.isPresent()) {
            Board board = boardOpt.get();
            // board 객체 대신 boardId 로 조회
            List<BoardFile> files = boardFileRepository.findByBoard_BoardId(boardId);
            board.setFiles(files);
            return board;
        }
        return null;
    }

    // 페이징 처리된 전체 게시글 조회 (새로 추가)
    public Page<Board> findAll(Pageable pageable) {
        return boardRepository.findAll(pageable);
    }

    // 카테고리별 페이징 조회 (새로 추가)
    public Page<Board> findByCategory(String category, Pageable pageable) {
        return boardRepository.findByCategory(category, pageable);
    }

    // 기존 메서드 (하위 호환성을 위해 유지)
    public List<Board> findAll() {
        return boardRepository.findAll();
    }

    // 게시글 수정
    public Board updateBoard(Board board, MultipartFile file) throws IOException {
        // 기존 게시글 저장 (업데이트)
        Board updatedBoard = boardRepository.save(board);

        // 새 파일이 있으면 추가 저장
        if (file != null && !file.isEmpty()) {
            // 업로드 경로
            String uploadDir = "C://upload/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 파일명 충돌 방지
            String savedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String filePath = uploadDir + savedFileName;

            // 실제 파일 저장
            file.transferTo(new File(filePath));

            // DB에 파일 정보 저장
            BoardFile boardFile = new BoardFile();
            boardFile.setBoard(updatedBoard);
            boardFile.setFileName(file.getOriginalFilename());
            boardFile.setFileUrl("/files/" + savedFileName);
            boardFileRepository.save(boardFile);
        }

        // 파일까지 포함해서 다시 조회 후 반환
        return findByIdWithFiles(updatedBoard.getBoardId());
    }

    // 게시글 삭제
    public void deleteBoard(Long boardId) {
        // 연관된 파일들 먼저 삭제 (CASCADE 설정되어 있으면 자동으로 삭제됨)
        List<BoardFile> files = boardFileRepository.findByBoard_BoardId(boardId);

        // 실제 파일 시스템에서 파일 삭제
        for (BoardFile file : files) {
            try {
                String fileName = file.getFileUrl().replace("/files/", "");
                File physicalFile = new File("C://upload/" + fileName);
                if (physicalFile.exists()) {
                    physicalFile.delete();
                }
            } catch (Exception e) {
                // 파일 삭제 실패해도 DB는 삭제 진행
                e.printStackTrace();
            }
        }

        // DB에서 게시글 삭제 (CASCADE로 파일 정보도 함께 삭제됨)
        boardRepository.deleteById(boardId);
    }
}