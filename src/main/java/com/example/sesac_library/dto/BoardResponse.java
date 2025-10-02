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
    private Long userId; // ✅ Integer → Long 변경
    private String userName; // ✅ 작성자 이름 추가
    private String category;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private Integer views;
    private List<String> fileUrls;

    public static BoardResponse from(Board board, String userName) {
        BoardResponse response = new BoardResponse();
        response.setBoardId(board.getBoardId());
        response.setUserId(board.getUserId());
        response.setUserName(userName); // ✅ 작성자 이름 설정
        response.setCategory(board.getCategory());
        response.setTitle(board.getTitle());
        response.setContent(board.getContent());
        response.setCreatedAt(board.getCreatedAt());
        response.setViews(board.getViews());

        if (board.getFiles() != null) {
            response.setFileUrls(board.getFiles().stream()
                    .map(BoardFile::getFileUrl)
                    .collect(Collectors.toList()));
        } else {
            response.setFileUrls(List.of());
        }

        return response;
    }
}