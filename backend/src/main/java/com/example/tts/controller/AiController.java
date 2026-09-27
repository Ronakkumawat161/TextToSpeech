package com.example.tts.controller;

import com.example.tts.dto.AiEnhanceRequest;
import com.example.tts.dto.AiEnhanceResponse;
import com.example.tts.service.AiTextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiTextService aiTextService;

    /**
     * Enhance input text with AI operations: SUMMARIZE, CORRECT_GRAMMAR, REWRITE, CONVERSATIONAL.
     * POST /api/ai/enhance
     */
    @PostMapping(
            value = "/enhance",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AiEnhanceResponse> enhanceText(@Valid @RequestBody AiEnhanceRequest request) {
        log.info("Received AI enhancement request: operation='{}', textLength={}",
                request.getOperation(), request.getText() != null ? request.getText().length() : 0);

        AiEnhanceResponse response = aiTextService.enhanceText(request);
        return ResponseEntity.ok(response);
    }
}
