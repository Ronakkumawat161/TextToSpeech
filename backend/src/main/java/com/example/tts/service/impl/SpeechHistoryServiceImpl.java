package com.example.tts.service.impl;

import com.example.tts.dto.SpeechHistoryDto;
import com.example.tts.exception.ForbiddenException;
import com.example.tts.exception.InvalidRequestException;
import com.example.tts.exception.ResourceNotFoundException;
import com.example.tts.exception.ServiceUnavailableException;
import com.example.tts.model.SpeechHistory;
import com.example.tts.model.User;
import com.example.tts.repository.SpeechHistoryRepository;
import com.example.tts.service.AuthService;
import com.example.tts.service.SpeechHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing Speech History with user isolation.
 * Ensures users only access and manage their own speech records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechHistoryServiceImpl implements SpeechHistoryService {

    private final SpeechHistoryRepository historyRepository;
    private final AuthService authService;

    @Override
    public SpeechHistoryDto saveHistory(
            String text,
            String language,
            String voice,
            String audioUrl,
            String storageReference,
            int characterCount) {
        return saveHistory(text, language, voice, audioUrl, storageReference, characterCount, 1.0, 0, 1.0, "default");
    }

    @Override
    public SpeechHistoryDto saveHistory(
            String text,
            String language,
            String voice,
            String audioUrl,
            String storageReference,
            int characterCount,
            Double speed,
            Integer pitch,
            Double volume,
            String style) {

        User currentUser = null;
        try {
            currentUser = authService.getCurrentUserEntity();
        } catch (Exception ex) {
            log.debug("No authenticated user session found during saveHistory: {}", ex.getMessage());
        }

        return saveHistory(text, language, voice, audioUrl, storageReference, characterCount, speed, pitch, volume, style, currentUser);
    }

    @Override
    public SpeechHistoryDto saveHistory(
            String text,
            String language,
            String voice,
            String audioUrl,
            String storageReference,
            int characterCount,
            User user) {
        return saveHistory(text, language, voice, audioUrl, storageReference, characterCount, 1.0, 0, 1.0, "default", user);
    }

    @Override
    public SpeechHistoryDto saveHistory(
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
            User user) {

        try {
            SpeechHistory entity = SpeechHistory.builder()
                    .user(user)
                    .text(text)
                    .language(language)
                    .voice(voice)
                    .audioUrl(audioUrl)
                    .storageReference(storageReference)
                    .characterCount(characterCount)
                    .speed(speed != null ? speed : 1.0)
                    .pitch(pitch != null ? pitch : 0)
                    .volume(volume != null ? volume : 1.0)
                    .style(style != null ? style : "default")
                    .createdAt(LocalDateTime.now())
                    .build();

            SpeechHistory saved = historyRepository.save(entity);
            log.info("Persisted speech history ID: {} for user: {}",
                    saved.getId(), user != null ? user.getEmail() : "anonymous");
            return mapToDto(saved);

        } catch (Exception ex) {
            // Graceful failure: speech synthesis flow continues uninterrupted if DB is unavailable
            log.warn("Database persistence failed: {}. Synthesis flow will continue uninterrupted.", ex.getMessage());
            return SpeechHistoryDto.builder()
                    .userId(user != null ? user.getId() : null)
                    .userEmail(user != null ? user.getEmail() : null)
                    .text(text)
                    .language(language)
                    .voice(voice)
                    .audioUrl(audioUrl)
                    .storageReference(storageReference)
                    .characterCount(characterCount)
                    .speed(speed != null ? speed : 1.0)
                    .pitch(pitch != null ? pitch : 0)
                    .volume(volume != null ? volume : 1.0)
                    .style(style != null ? style : "default")
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }


    @Override
    public List<SpeechHistoryDto> getAllHistory() {
        try {
            User currentUser = authService.getCurrentUserEntity();
            List<SpeechHistory> list;

            if (currentUser != null) {
                // Enforce user isolation: only retrieve the authenticated user's records
                list = historyRepository.findAllByUserIdOrderByCreatedAtDesc(currentUser.getId());
            } else {
                list = historyRepository.findAllByOrderByCreatedAtDesc();
            }

            return list.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

        } catch (Exception ex) {
            log.warn("Database unavailable during getAllHistory: {}", ex.getMessage());
            throw new ServiceUnavailableException("Database service is temporarily unavailable. Please verify PostgreSQL connection.");
        }
    }

    @Override
    public SpeechHistoryDto getHistoryById(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidRequestException("Invalid history ID: '" + id + "'. ID must be a positive integer.");
        }

        try {
            SpeechHistory entity = historyRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("SpeechHistory", "id", id));

            // Enforce user isolation: user A cannot access user B's history
            checkOwnership(entity, id, "access");

            return mapToDto(entity);

        } catch (ResourceNotFoundException | InvalidRequestException | ForbiddenException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Database unavailable during getHistoryById({}): {}", id, ex.getMessage());
            throw new ServiceUnavailableException("Database service is temporarily unavailable. Please verify PostgreSQL connection.");
        }
    }

    @Override
    public void deleteHistoryById(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidRequestException("Invalid history ID: '" + id + "'. ID must be a positive integer.");
        }

        try {
            SpeechHistory entity = historyRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("SpeechHistory", "id", id));

            // Enforce user isolation: user A cannot delete user B's history
            checkOwnership(entity, id, "delete");

            historyRepository.delete(entity);
            log.info("Deleted speech history record with ID: {}", id);

        } catch (ResourceNotFoundException | InvalidRequestException | ForbiddenException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Database unavailable during deleteHistoryById({}): {}", id, ex.getMessage());
            throw new ServiceUnavailableException("Database service is temporarily unavailable. Please verify PostgreSQL connection.");
        }
    }

    @Override
    public SpeechHistoryDto toggleFavorite(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidRequestException("Invalid history ID: '" + id + "'. ID must be a positive integer.");
        }

        try {
            SpeechHistory entity = historyRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("SpeechHistory", "id", id));

            checkOwnership(entity, id, "update favorite status on");

            boolean currentFav = Boolean.TRUE.equals(entity.getFavorite());
            entity.setFavorite(!currentFav);

            SpeechHistory updated = historyRepository.save(entity);
            log.info("Toggled favorite status for speech history ID {} to {}", id, updated.getFavorite());
            return mapToDto(updated);

        } catch (ResourceNotFoundException | InvalidRequestException | ForbiddenException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Database unavailable during toggleFavorite({}): {}", id, ex.getMessage());
            throw new ServiceUnavailableException("Database service is temporarily unavailable. Please verify PostgreSQL connection.");
        }
    }

    private void checkOwnership(SpeechHistory entity, Long id, String action) {
        User currentUser = authService.getCurrentUserEntity();
        if (entity.getUser() != null) {
            if (currentUser == null || !entity.getUser().getId().equals(currentUser.getId())) {
                String currentEmail = currentUser != null ? currentUser.getEmail() : "unauthenticated";
                Long currentId = currentUser != null ? currentUser.getId() : null;
                log.warn("Security violation: User '{}' (ID {}) attempted to {} history record {} owned by User '{}' (ID {})",
                        currentEmail, currentId, action, id,
                        entity.getUser().getEmail(), entity.getUser().getId());
                throw new ForbiddenException("You do not have permission to " + action + " this speech history record");
            }
        }
    }

    private SpeechHistoryDto mapToDto(SpeechHistory entity) {
        return SpeechHistoryDto.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .userEmail(entity.getUser() != null ? entity.getUser().getEmail() : null)
                .text(entity.getText())
                .language(entity.getLanguage())
                .voice(entity.getVoice())
                .audioUrl(entity.getAudioUrl())
                .storageReference(entity.getStorageReference())
                .characterCount(entity.getCharacterCount())
                .speed(entity.getSpeed() != null ? entity.getSpeed() : 1.0)
                .pitch(entity.getPitch() != null ? entity.getPitch() : 0)
                .volume(entity.getVolume() != null ? entity.getVolume() : 1.0)
                .style(entity.getStyle() != null ? entity.getStyle() : "default")
                .favorite(Boolean.TRUE.equals(entity.getFavorite()))
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

