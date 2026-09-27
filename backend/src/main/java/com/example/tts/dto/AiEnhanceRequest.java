package com.example.tts.dto;

import com.example.tts.model.AiOperation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiEnhanceRequest {

    @NotNull(message = "Text is required")
    @NotBlank(message = "Text cannot be blank")
    @Size(max = 5000, message = "Text cannot exceed 5000 characters")
    private String text;

    @NotNull(message = "Operation is required")
    private AiOperation operation;
}
