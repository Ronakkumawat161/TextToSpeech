package com.example.tts.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for SpeechHistory records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechHistoryDto {

    private Long id;
    private Long userId;
    private String userEmail;
    private String text;
    private String language;
    private String voice;
    private String audioUrl;
    private String storageReference;
    private Integer characterCount;

    @Builder.Default
    private Double speed = 1.0;

    @Builder.Default
    private Integer pitch = 0;

    @Builder.Default
    private Double volume = 1.0;

    @Builder.Default
    private String style = "default";

    @Builder.Default
    private Boolean favorite = false;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

}
