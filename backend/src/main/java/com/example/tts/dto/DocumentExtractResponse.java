package com.example.tts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentExtractResponse {

    @Builder.Default
    private boolean success = true;

    private String text;
    private String filename;
    private String fileType;
    private long characterCount;
    private long wordCount;
    private long fileSize;
    private String message;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
