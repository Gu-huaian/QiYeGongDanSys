package com.example.qigd_ljl.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {

    private Long id;
    private String fileName;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String filePath;
    private UserSimpleResponse uploader;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSimpleResponse {
        private Long id;
        private String username;
        private String realName;
    }
}