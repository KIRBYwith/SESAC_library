package com.example.sesac_library.repository;

import com.example.sesac_library.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    // 기존 메서드들
    List<Board> findAllByOrderByCreatedAtDesc();

    List<Board> findByCategoryOrderByCreatedAtDesc(String category);

    @Modifying
    @Query("UPDATE Board b SET b.views = b.views + 1 WHERE b.boardId = :boardId")
    void incrementViews(@Param("boardId") Long boardId);

    // 페이징 처리를 위한 새 메서드들
    Page<Board> findByCategory(String category, Pageable pageable);

    // findAll(Pageable pageable)은 JpaRepository에서 상속받으므로 선언 불필요
}