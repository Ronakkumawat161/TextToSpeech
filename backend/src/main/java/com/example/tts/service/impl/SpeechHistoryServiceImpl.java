package com.example.tts.service.impl;

import com.example.tts.config.DemoAuthProperties;
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
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service implementation for managing Speech History with user isolation.
 * Ensures users only access and manage their own speech records.
 * Provides in-memory development history for the demo account when PostgreSQL is unavailable.
 */
@Slf4j
@Service
public class SpeechHistoryServiceImpl implements SpeechHistoryService {

    private final SpeechHistoryRepository historyRepository;
    private final AuthService authService;
    private final DemoAuthProperties demoAuthProperties;

    // Development-only in-memory storage for demo user history when database is offline
    private final ConcurrentMap<Long, SpeechHistoryDto> inMemoryDemoHistory = new ConcurrentHashMap<>();
    private final AtomicLong demoIdCounter = new AtomicLong(5000);

    public SpeechHistoryServiceImpl(SpeechHistoryRepository historyRepository, AuthService authService) {
        this(historyRepository, authService, new DemoAuthProperties());
    }

    @Autowired
    public SpeechHistoryServiceImpl(
            SpeechHistoryRepository historyRepository,
            AuthService authService,
            DemoAuthProperties demoAuthProperties) {
        this.historyRepository = historyRepository;
        this.authService = authService;
        this.demoAuthProperties = demoAuthProperties != null ? demoAuthProperties : new DemoAuthProperties();
        initDemoHistory();
    }

    @PostConstruct
    public void initDemoHistory() {
        if (demoAuthProperties.isEnabled()) {
            long id1 = demoIdCounter.incrementAndGet();
            inMemoryDemoHistory.put(id1, SpeechHistoryDto.builder()
                    .id(id1)
                    .userId(-1L)
                    .userEmail(demoAuthProperties.getEmail())
                    .text("Welcome to VoxFlow Speech Studio! This is a pre-loaded speech sample in your development session.")
                    .language("en-US")
                    .voice("en-US-JennyNeural")
                    .characterCount(88)
                    .speed(1.0)
                    .pitch(0)
                    .volume(1.0)
                    .style("cheerful")
                    .favorite(true)
                    .createdAt(LocalDateTime.now().minusHours(2))
                    .build());

            long id2 = demoIdCounter.incrementAndGet();
            inMemoryDemoHistory.put(id2, SpeechHistoryDto.builder()
                    .id(id2)
                    .userId(-1L)
                    .userEmail(demoAuthProperties.getEmail())
                    .text("Artificial intelligence and neural speech synthesis bring text to life with human-like intonation.")
                    .language("en-US")
                    .voice("en-US-GuyNeural")
                    .characterCount(99)
                    .speed(1.0)
                    .pitch(0)
                    .volume(1.0)
                    .style("default")
                    .favorite(false)
                    .createdAt(LocalDateTime.now().minusMinutes(35))
                    .build());
        }
    }

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
            log.warn("Database persistence failed: {}. Synthesis flow will continue uninterrupted.", ex.getMessage());

            // If demo auth is enabled and user is the demo user, save to in-memory history
            if (demoAuthProperties.isEnabled() && user != null && user.getEmail() != null
                    && user.getEmail().equalsIgnoreCase(demoAuthProperties.getEmail())) {
                long inMemoryId = demoIdCounter.incrementAndGet();
                SpeechHistoryDto inMemoryDto = SpeechHistoryDto.builder()
                        .id(inMemoryId)
                        .userId(user.getId())
                        .userEmail(user.getEmail())
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
                        .favorite(false)
                        .createdAt(LocalDateTime.now())
                        .build();
                inMemoryDemoHistory.put(inMemoryId, inMemoryDto);
                log.info("Recorded in-memory demo history ID: {} for user: {}", inMemoryId, user.getEmail());
                return inMemoryDto;
            }

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
        User currentUser = null;
        try {
            currentUser = authService.getCurrentUserEntity();
        } catch (Exception ex) {
            log.debug("Could not resolve current user entity: {}", ex.getMessage());
        }

        try {
            List<SpeechHistory> list;
            if (currentUser != null && currentUser.getId() != null && currentUser.getId() > 0) {
                list = historyRepository.findAllByUserIdOrderByCreatedAtDesc(currentUser.getId());
            } else {
                list = historyRepository.findAllByOrderByCreatedAtDesc();
            }

            return list.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

        } catch (Exception ex) {
            log.warn("Database unavailable during getAllHistory: {}", ex.getMessage());

            // If demo auth is enabled and user is demo user, return in-memory history
            if (demoAuthProperties.isEnabled() && currentUser != null && currentUser.getEmail() != null
                    && currentUser.getEmail().equalsIgnoreCase(demoAuthProperties.getEmail())) {
                log.info("Returning in-memory demo history for '{}' (count: {})",
                        currentUser.getEmail(), inMemoryDemoHistory.size());
                return inMemoryDemoHistory.values().stream()
                        .sorted(Comparator.comparing(SpeechHistoryDto::getCreatedAt).reversed())
                        .collect(Collectors.toList());
            }

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

            checkOwnership(entity, id, "access");
            return mapToDto(entity);

        } catch (ResourceNotFoundException | InvalidRequestException | ForbiddenException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Database unavailable during getHistoryById({}): {}", id, ex.getMessage());

            if (demoAuthProperties.isEnabled() && inMemoryDemoHistory.containsKey(id)) {
                return inMemoryDemoHistory.get(id);
            }

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

            checkOwnership(entity, id, "delete");
            historyRepository.delete(entity);
            log.info("Deleted speech history record with ID: {}", id);

        } catch (ResourceNotFoundException | InvalidRequestException | ForbiddenException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Database unavailable during deleteHistoryById({}): {}", id, ex.getMessage());

            if (demoAuthProperties.isEnabled() && inMemoryDemoHistory.containsKey(id)) {
                inMemoryDemoHistory.remove(id);
                log.info("Deleted in-memory demo speech history record with ID: {}", id);
                return;
            }

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

            if (demoAuthProperties.isEnabled() && inMemoryDemoHistory.containsKey(id)) {
                SpeechHistoryDto item = inMemoryDemoHistory.get(id);
                item.setFavorite(!Boolean.TRUE.equals(item.getFavorite()));
                return item;
            }

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

