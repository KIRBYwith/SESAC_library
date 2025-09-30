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
    private String category;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private Integer views;
    private List<String> fileUrls;

    public static BoardResponse from(Board board) {
        BoardResponse response = new BoardResponse();
        response.setBoardId(board.getBoardId());
        response.setUserId(board.getUserId());
        response.setCategory(board.getCategory());
        response.setTitle(board.getTitle());
        response.setContent(board.getContent());
        response.setCreatedAt(board.getCreatedAt());
        response.setViews(board.getViews());

        /* 이전
        if (board.getFiles() != null && !board.getFiles().isEmpty()) {
            response.setFileUrls(board.getFiles().stream()
                    .map(BoardFile::getFileUrl)
                    .collect(Collectors.toList()));
        }
        */

        // 수정
        if (board.getFiles() != null) {
            response.setFileUrls(board.getFiles().stream()
                    .map(BoardFile::getFileUrl)
                    .collect(Collectors.toList()));
        } else {
            response.setFileUrls(List.of()); // null 대신 빈 리스트
        }

        return response;
    }
}