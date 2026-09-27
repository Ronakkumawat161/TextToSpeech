package com.example.tts.controller;

import com.example.tts.dto.AudioSynthesisResult;
import com.example.tts.dto.TtsRequest;
import com.example.tts.dto.TtsResponse;
import com.example.tts.service.TtsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {

    private final TtsService ttsService;

    /**
     * Synthesize speech from text parameters.
     * Safely returns generated audio bytes directly with Content-Type: audio/mpeg (Option A).
     * Also supports application/json when explicitly requested via Accept header.
     *
     * POST /api/tts
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {"audio/mpeg", "application/octet-stream", "application/json"}
    )
    public ResponseEntity<?> synthesize(
            @Valid @RequestBody TtsRequest request,
            @RequestHeader(value = "Accept", required = false) String acceptHeader) {

        // If client explicitly requests application/json, return TtsResponse DTO
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            TtsResponse jsonResponse = ttsService.synthesize(request);
            return ResponseEntity.ok(jsonResponse);
        }

        // Primary: Return audio bytes directly (Option A) with audio/mpeg Content-Type
        AudioSynthesisResult result = ttsService.synthesizeAudio(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(result.getFilename())
                .build());
        headers.setContentLength(result.getAudioBytes().length);
        headers.setCacheControl(CacheControl.noCache().noStore().mustRevalidate());
        headers.setPragma("no-cache");
        headers.setExpires(0);

        // Informative headers for client telemetry
        headers.set("X-Audio-Format", result.getFormat());
        headers.set("X-Voice-Id", result.getVoice());
        headers.set("X-Language", result.getLanguage());
        headers.set("X-Character-Count", String.valueOf(result.getCharacterCount()));
        headers.set("X-Provider", result.getProvider());

        return new ResponseEntity<>(result.getAudioBytes(), headers, HttpStatus.OK);
    }
}
