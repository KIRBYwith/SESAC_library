package com.example.sesac_library.repository;

import com.example.sesac_library.entity.BoardFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardFileRepository extends JpaRepository<BoardFile, Long> {
    // board_id 로 파일 리스트 조회
    List<BoardFile> findByBoard_BoardId(Long boardId);
}
