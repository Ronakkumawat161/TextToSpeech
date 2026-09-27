package com.example.tts.controller;

import com.example.tts.dto.SpeechHistoryDto;
import com.example.tts.exception.ResourceNotFoundException;
import com.example.tts.exception.ServiceUnavailableException;
import com.example.tts.service.SpeechHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "testuser@example.com")
class SpeechHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpeechHistoryService historyService;

    @Test
    void testGetAllHistoryReturnsOkWithList() throws Exception {
        SpeechHistoryDto dto = SpeechHistoryDto.builder()
                .id(1L)
                .text("Test speech text")
                .language("en-US")
                .voice("en-female-1")
                .audioUrl("/api/tts/audio/speech-1.mp3")
                .characterCount(16)
                .createdAt(LocalDateTime.now())
                .build();

        when(historyService.getAllHistory()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/history"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].text").value("Test speech text"))
                .andExpect(jsonPath("$[0].voice").value("en-female-1"));
    }

    @Test
    void testGetHistoryByIdSuccess() throws Exception {
        SpeechHistoryDto dto = SpeechHistoryDto.builder()
                .id(42L)
                .text("Specific record")
                .language("en-US")
                .voice("en-male-1")
                .characterCount(15)
                .createdAt(LocalDateTime.now())
                .build();

        when(historyService.getHistoryById(42L)).thenReturn(dto);

        mockMvc.perform(get("/api/history/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.text").value("Specific record"))
                .andExpect(jsonPath("$.voice").value("en-male-1"));
    }

    @Test
    void testGetHistoryByIdNotFoundReturns404() throws Exception {
        when(historyService.getHistoryById(999L))
                .thenThrow(new ResourceNotFoundException("SpeechHistory", "id", 999L));

        mockMvc.perform(get("/api/history/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void testDeleteHistoryByIdSuccess() throws Exception {
        doNothing().when(historyService).deleteHistoryById(10L);

        mockMvc.perform(delete("/api/history/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message", containsString("deleted successfully")));

        verify(historyService, times(1)).deleteHistoryById(10L);
    }

    @Test
    void testDeleteHistoryByIdNotFoundReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("SpeechHistory", "id", 404L))
                .when(historyService).deleteHistoryById(404L);

        mockMvc.perform(delete("/api/history/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void testDatabaseUnavailableReturns503() throws Exception {
        when(historyService.getAllHistory())
                .thenThrow(new ServiceUnavailableException("Database service is temporarily unavailable."));

        mockMvc.perform(get("/api/history"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.error.message", containsString("temporarily unavailable")));
    }

    @Test
    void testGetHistoryByIdInvalidNegativeIdReturns400() throws Exception {
        mockMvc.perform(get("/api/history/-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message", containsString("positive integer")));
    }

    @Test
    void testGetHistoryByIdInvalidZeroIdReturns400() throws Exception {
        mockMvc.perform(get("/api/history/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message", containsString("positive integer")));
    }

    @Test
    void testGetHistoryByIdNonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/history/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void testDeleteHistoryByIdInvalidNegativeIdReturns400() throws Exception {
        mockMvc.perform(delete("/api/history/-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void testToggleFavoriteSuccess() throws Exception {
        SpeechHistoryDto dto = SpeechHistoryDto.builder()
                .id(7L)
                .text("Favorite speech")
                .favorite(true)
                .build();

        when(historyService.toggleFavorite(7L)).thenReturn(dto);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/history/7/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.favorite").value(true));
    }
}
