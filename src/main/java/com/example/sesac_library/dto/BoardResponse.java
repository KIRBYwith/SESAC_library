package com.example.sesac_library.dto;

import lombok.Getter;
import lombok.Setter;
import com.example.sesac_library.entity.Board;
import com.example.sesac_library.entity.BoardFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class BoardResponse {
    private Long boardId;
    private Long userId;
    private String userName;
    private String category;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private Integer views;

    // ✅ 프론트엔드 호환을 위해 files 필드 추가
    private List<FileInfo> files;

    // 파일 정보를 담는 내부 클래스
    @Getter
    @Setter
    public static class FileInfo {
        private String fileName;
        private String fileUrl;

        public FileInfo(String fileName, String fileUrl) {
            this.fileName = fileName;
            this.fileUrl = fileUrl;
        }
    }

    public static BoardResponse from(Board board, String userName) {
        BoardResponse response = new BoardResponse();
        response.setBoardId(board.getBoardId());
        response.setUserId(board.getUserId());
        response.setUserName(userName);
        response.setCategory(board.getCategory());
        response.setTitle(board.getTitle());
        response.setContent(board.getContent());
        response.setCreatedAt(board.getCreatedAt());
        response.setViews(board.getViews());

        // ✅ 파일 정보를 fileName과 fileUrl 포함한 객체로 변환
        if (board.getFiles() != null && !board.getFiles().isEmpty()) {
            response.setFiles(board.getFiles().stream()
                    .map(file -> new FileInfo(file.getFileName(), file.getFileUrl()))
                    .collect(Collectors.toList()));
        } else {
            response.setFiles(new ArrayList<>());
        }

        return response;
    }
}