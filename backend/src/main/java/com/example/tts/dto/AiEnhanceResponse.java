package com.example.tts.dto;

import com.example.tts.model.AiOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiEnhanceResponse {

    @Builder.Default
    private boolean success = true;

    private String originalText;
    private String enhancedText;
    private AiOperation operation;
    private String provider;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
