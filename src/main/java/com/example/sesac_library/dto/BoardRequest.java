package com.example.sesac_library.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class BoardRequest {
    private Integer userId;  // Integer로 통일
    private String category;
    private String title;
    private String content;
    private String passwordHash;
}