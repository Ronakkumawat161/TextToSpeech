package com.example.tts.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsRequest {

    @NotNull(message = "Text is required")
    @NotBlank(message = "Text cannot be blank")
    @Size(max = 5000, message = "Text cannot exceed 5000 characters")
    private String text;

    @NotNull(message = "Language is required")
    @NotBlank(message = "Language cannot be blank")
    private String language;

    @NotNull(message = "Voice is required")
    @NotBlank(message = "Voice cannot be blank")
    private String voice;

    @DecimalMin(value = "0.25", message = "Speaking speed must be at least 0.25x")
    @DecimalMax(value = "4.0", message = "Speaking speed cannot exceed 4.0x")
    @Builder.Default
    private Double speed = 1.0;

    @Min(value = -100, message = "Pitch cannot be less than -100%")
    @Max(value = 100, message = "Pitch cannot exceed +100%")
    @Builder.Default
    private Integer pitch = 0;

    @DecimalMin(value = "0.0", message = "Volume must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Volume cannot exceed 1.0")
    @Builder.Default
    private Double volume = 1.0;

    @Size(max = 50, message = "Voice style cannot exceed 50 characters")
    @Builder.Default
    private String style = "default";

    public TtsRequest(String text, String language, String voice) {
        this.text = text;
        this.language = language;
        this.voice = voice;
        this.speed = 1.0;
        this.pitch = 0;
        this.volume = 1.0;
        this.style = "default";
    }
}

