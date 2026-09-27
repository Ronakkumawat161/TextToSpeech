package com.example.tts.service;

import com.example.tts.dto.SpeechHistoryDto;
import com.example.tts.exception.ForbiddenException;
import com.example.tts.exception.InvalidRequestException;
import com.example.tts.exception.ResourceNotFoundException;
import com.example.tts.exception.ServiceUnavailableException;
import com.example.tts.model.Role;
import com.example.tts.model.SpeechHistory;
import com.example.tts.model.User;
import com.example.tts.repository.SpeechHistoryRepository;
import com.example.tts.service.impl.SpeechHistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpeechHistoryServiceTest {

    @Mock
    private SpeechHistoryRepository historyRepository;

    @Mock
    private AuthService authService;

    private SpeechHistoryService historyService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        historyService = new SpeechHistoryServiceImpl(historyRepository, authService);

        userA = User.builder()
                .id(100L)
                .email("user.a@example.com")
                .name("User A")
                .role(Role.ROLE_USER)
                .build();

        userB = User.builder()
                .id(200L)
                .email("user.b@example.com")
                .name("User B")
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    void testSaveHistorySuccess() {
        when(authService.getCurrentUserEntity()).thenReturn(userA);

        SpeechHistory savedEntity = SpeechHistory.builder()
                .id(1L)
                .user(userA)
                .text("Hello world")
                .language("en-US")
                .voice("en-female-1")
                .audioUrl("/api/tts/audio/speech-1.mp3")
                .storageReference("speech-1.mp3")
                .characterCount(11)
                .createdAt(LocalDateTime.now())
                .build();

        when(historyRepository.save(any(SpeechHistory.class))).thenReturn(savedEntity);

        SpeechHistoryDto dto = historyService.saveHistory(
                "Hello world",
                "en-US",
                "en-female-1",
                "/api/tts/audio/speech-1.mp3",
                "speech-1.mp3",
                11
        );

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(100L, dto.getUserId());
        assertEquals("user.a@example.com", dto.getUserEmail());
        assertEquals("Hello world", dto.getText());
        assertEquals("en-US", dto.getLanguage());
        verify(historyRepository, times(1)).save(any(SpeechHistory.class));
    }

    @Test
    void testSaveHistoryHandlesDatabaseUnavailableGracefully() {
        when(authService.getCurrentUserEntity()).thenReturn(userA);
        when(historyRepository.save(any(SpeechHistory.class)))
                .thenThrow(new DataAccessResourceFailureException("PostgreSQL connection refused"));

        SpeechHistoryDto dto = historyService.saveHistory(
                "Hello world",
                "en-US",
                "en-female-1",
                "/api/tts/audio/speech-1.mp3",
                "speech-1.mp3",
                11
        );

        assertNotNull(dto);
        assertEquals("Hello world", dto.getText());
        assertNull(dto.getId()); // ID is null because DB was offline, but synthesis succeeded
    }

    @Test
    void testGetAllHistoryForAuthenticatedUser() {
        when(authService.getCurrentUserEntity()).thenReturn(userA);

        SpeechHistory item = SpeechHistory.builder()
                .id(1L)
                .user(userA)
                .text("Sample text")
                .language("en-US")
                .voice("en-female-1")
                .createdAt(LocalDateTime.now())
                .build();

        when(historyRepository.findAllByUserIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(item));

        List<SpeechHistoryDto> result = historyService.getAllHistory();

        assertEquals(1, result.size());
        assertEquals("Sample text", result.get(0).getText());
        assertEquals(100L, result.get(0).getUserId());
        verify(historyRepository, times(1)).findAllByUserIdOrderByCreatedAtDesc(100L);
    }

    @Test
    void testGetAllHistoryDatabaseUnavailableThrowsServiceUnavailable() {
        when(authService.getCurrentUserEntity()).thenReturn(userA);
        when(historyRepository.findAllByUserIdOrderByCreatedAtDesc(100L))
                .thenThrow(new DataAccessResourceFailureException("Database offline"));

        assertThrows(ServiceUnavailableException.class, () -> historyService.getAllHistory());
    }

    @Test
    void testGetHistoryByIdSuccessForOwner() {
        when(authService.getCurrentUserEntity()).thenReturn(userA);

        SpeechHistory item = SpeechHistory.builder()
                .id(5L)
                .user(userA)
                .text("Found text")
                .language("en-US")
                .voice("en-female-1")
                .createdAt(LocalDateTime.now())
                .build();

        when(historyRepository.findById(5L)).thenReturn(Optional.of(item));

        SpeechHistoryDto dto = historyService.getHistoryById(5L);
        assertNotNull(dto);
        assertEquals(5L, dto.getId());
        assertEquals("Found text", dto.getText());
    }

    @Test
    void testUserACannotAccessUserBHistory() {
        // Logged-in user is User B
        when(authService.getCurrentUserEntity()).thenReturn(userB);

        // Record belongs to User A
        SpeechHistory itemOwnedByUserA = SpeechHistory.builder()
                .id(5L)
                .user(userA)
                .text("User A's private text")
                .language("en-US")
                .voice("en-female-1")
                .createdAt(LocalDateTime.now())
                .build();

        when(historyRepository.findById(5L)).thenReturn(Optional.of(itemOwnedByUserA));

        // Attempting to access User A's record must throw ForbiddenException
        assertThrows(ForbiddenException.class, () -> historyService.getHistoryById(5L));
    }

    @Test
    void testUserACannotDeleteUserBHistory() {
        // Logged-in user is User B
        when(authService.getCurrentUserEntity()).thenReturn(userB);

        // Record belongs to User A
        SpeechHistory itemOwnedByUserA = SpeechHistory.builder()
                .id(5L)
                .user(userA)
                .text("User A's private text")
                .language("en-US")
                .voice("en-female-1")
                .createdAt(LocalDateTime.now())
                .build();

        when(historyRepository.findById(5L)).thenReturn(Optional.of(itemOwnedByUserA));

        // Attempting to delete User A's record must throw ForbiddenException
        assertThrows(ForbiddenException.class, () -> historyService.deleteHistoryById(5L));
        verify(historyRepository, never()).delete(any());
    }

    @Test
    void testGetHistoryByIdNotFoundThrowsResourceNotFoundException() {
        when(historyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> historyService.getHistoryById(999L));
    }

    @Test
    void testGetHistoryByIdInvalidIdThrowsInvalidRequestException() {
        assertThrows(InvalidRequestException.class, () -> historyService.getHistoryById(null));
        assertThrows(InvalidRequestException.class, () -> historyService.getHistoryById(-1L));
        assertThrows(InvalidRequestException.class, () -> historyService.getHistoryById(0L));
    }

    @Test
    void testDeleteHistoryByIdSuccessForOwner() {
        when(authService.getCurrentUserEntity()).thenReturn(userA);

        SpeechHistory item = SpeechHistory.builder()
                .id(3L)
                .user(userA)
                .text("Owner record")
                .build();

        when(historyRepository.findById(3L)).thenReturn(Optional.of(item));
        doNothing().when(historyRepository).delete(item);

        assertDoesNotThrow(() -> historyService.deleteHistoryById(3L));
        verify(historyRepository, times(1)).delete(item);
    }

    @Test
    void testToggleFavoriteSuccessForOwner() {
        when(authService.getCurrentUserEntity()).thenReturn(userA);

        SpeechHistory item = SpeechHistory.builder()
                .id(9L)
                .user(userA)
                .text("Owner record")
                .favorite(false)
                .build();

        when(historyRepository.findById(9L)).thenReturn(Optional.of(item));
        when(historyRepository.save(any(SpeechHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        SpeechHistoryDto result = historyService.toggleFavorite(9L);

        assertNotNull(result);
        assertTrue(result.getFavorite());
        verify(historyRepository, times(1)).save(any(SpeechHistory.class));
    }
}
