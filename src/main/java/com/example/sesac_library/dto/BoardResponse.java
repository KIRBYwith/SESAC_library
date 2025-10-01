package com.example.sesac_library.dto;

import lombok.Getter;
import lombok.Setter;
import com.example.sesac_library.entity.Board;
import com.example.sesac_library.entity.BoardFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class BoardResponse {
    private Long boardId;
    private Integer userId;
    private String userName;  // 작성자 이름
    private String category;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private Integer views;
    private List<String> fileUrls;

    // ✅ userName을 포함한 from 메서드 (단일 메서드만 유지)
    public static BoardResponse from(Board board, String userName) {
        BoardResponse response = new BoardResponse();
        response.boardId = board.getBoardId();
        response.userId = board.getUserId();
        response.userName = userName; // 작성자 이름 설정
        response.category = board.getCategory();
        response.title = board.getTitle();
        response.content = board.getContent();
        response.createdAt = board.getCreatedAt();
        response.views = board.getViews();

        if (board.getFiles() != null && !board.getFiles().isEmpty()) {
            response.fileUrls = board.getFiles().stream()
                    .map(BoardFile::getFileUrl)
                    .collect(Collectors.toList());
        } else {
            response.fileUrls = List.of();
        }

        return response;
    }
}