package com.example.sesac_library.repository;

import com.example.sesac_library.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findAllByOrderByCreatedAtDesc();

    List<Board> findByCategoryOrderByCreatedAtDesc(String category);

    @Modifying
    @Query("UPDATE Board b SET b.views = b.views + 1 WHERE b.boardId = :boardId")
    void incrementViews(@Param("boardId") Long boardId);
}
