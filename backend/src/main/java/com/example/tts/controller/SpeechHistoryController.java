package com.example.tts.controller;

import com.example.tts.dto.SpeechHistoryDto;
import com.example.tts.service.SpeechHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing Speech History.
 * 
 * Endpoints:
 * - GET /api/history : Retrieve all history records
 * - GET /api/history/{id} : Retrieve single history record
 * - DELETE /api/history/{id} : Delete history record
 */
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class SpeechHistoryController {

    private final SpeechHistoryService historyService;

    /**
     * Retrieve all synthesized speech history items (newest first).
     * GET /api/history
     */
    @GetMapping
    public ResponseEntity<List<SpeechHistoryDto>> getAllHistory() {
        List<SpeechHistoryDto> history = historyService.getAllHistory();
        return ResponseEntity.ok(history);
    }

    /**
     * Retrieve a specific speech history item by its ID.
     * GET /api/history/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SpeechHistoryDto> getHistoryById(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            throw new com.example.tts.exception.InvalidRequestException("Invalid history ID: '" + id + "'. ID must be a positive integer.");
        }
        SpeechHistoryDto item = historyService.getHistoryById(id);
        return ResponseEntity.ok(item);
    }

    /**
     * Delete a speech history item by its ID.
     * DELETE /api/history/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteHistoryById(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            throw new com.example.tts.exception.InvalidRequestException("Invalid history ID: '" + id + "'. ID must be a positive integer.");
        }
        historyService.deleteHistoryById(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Speech history record " + id + " deleted successfully"
        ));
    }

    /**
     * Toggle favorite status of a speech history record.
     * PATCH /api/history/{id}/favorite
     */
    @PatchMapping("/{id}/favorite")
    public ResponseEntity<SpeechHistoryDto> toggleFavorite(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            throw new com.example.tts.exception.InvalidRequestException("Invalid history ID: '" + id + "'. ID must be a positive integer.");
        }
        SpeechHistoryDto updated = historyService.toggleFavorite(id);
        return ResponseEntity.ok(updated);
    }
}
