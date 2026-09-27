package com.example.tts.service;

import com.example.tts.dto.SpeechHistoryDto;
import com.example.tts.model.User;

import java.util.List;

/**
 * Service interface for Speech History persistence and retrieval with User isolation.
 */
public interface SpeechHistoryService {

    /**
     * Persist metadata for a synthesized speech session.
     * Handled gracefully if database is temporarily unavailable.
     */
    default SpeechHistoryDto saveHistory(
            String text,
            String language,
            String voice,
            String audioUrl,
            String storageReference,
            int characterCount) {
        return saveHistory(text, language, voice, audioUrl, storageReference, characterCount, 1.0, 0, 1.0, "default");
    }

    SpeechHistoryDto saveHistory(
            String text,
            String language,
            String voice,
            String audioUrl,
            String storageReference,
            int characterCount,
            Double speed,
            Integer pitch,
            Double volume,
            String style
    );

    /**
     * Persist metadata for a synthesized speech session associated with an authenticated User.
     */
    default SpeechHistoryDto saveHistory(
            String text,
            String language,
            String voice,
            String audioUrl,
            String storageReference,
            int characterCount,
            User user) {
        return saveHistory(text, language, voice, audioUrl, storageReference, characterCount, 1.0, 0, 1.0, "default", user);
    }

    SpeechHistoryDto saveHistory(
            String text,
            String language,
            String voice,
            String audioUrl,
            String storageReference,
            int characterCount,
            Double speed,
            Integer pitch,
            Double volume,
            String style,
            User user
    );


    /**
     * Retrieve all speech history records for the current authenticated user ordered newest first.
     */
    List<SpeechHistoryDto> getAllHistory();

    /**
     * Retrieve a single speech history record by its ID, enforcing user ownership.
     */
    SpeechHistoryDto getHistoryById(Long id);

    /**
     * Delete a speech history record by its ID, enforcing user ownership.
     */
    void deleteHistoryById(Long id);

    /**
     * Toggle favorite status of a speech history record, enforcing user ownership.
     */
    SpeechHistoryDto toggleFavorite(Long id);
}
