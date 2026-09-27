package com.example.tts.controller;

import com.example.tts.dto.VoiceResponse;
import com.example.tts.service.TtsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/voices")
@RequiredArgsConstructor
public class VoiceController {

    private final TtsService ttsService;

    /**
     * Retrieve available voices list, with optional language query filter
     * GET /api/voices
     * GET /api/voices?language=en-US
     */
    @GetMapping
    public ResponseEntity<List<VoiceResponse>> getVoices(
            @RequestParam(required = false) String language) {
        if (language != null && !language.isBlank()) {
            return ResponseEntity.ok(ttsService.getVoicesByLanguage(language));
        }
        return ResponseEntity.ok(ttsService.getAvailableVoices());
    }
}
