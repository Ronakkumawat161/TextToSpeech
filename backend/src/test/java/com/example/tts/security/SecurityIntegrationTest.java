package com.example.tts.security;

import com.example.tts.dto.SpeechHistoryDto;
import com.example.tts.exception.ForbiddenException;
import com.example.tts.service.SpeechHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpeechHistoryService historyService;

    @Test
    void testPublicEndpointHealthAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testPublicEndpointVoicesAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/voices"))
                .andExpect(status().isOk());
    }

    @Test
    void testProtectedEndpointHistoryWithoutTokenReturns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message", containsString("Full authentication is required")));
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"USER"})
    void testProtectedEndpointHistoryWithTokenReturns200Ok() throws Exception {
        when(historyService.getAllHistory()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/history"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "userB@example.com", roles = {"USER"})
    void testUserBCannotAccessUserAHistoryReturns403Forbidden() throws Exception {
        when(historyService.getHistoryById(42L))
                .thenThrow(new ForbiddenException("You do not have permission to access this speech history record"));

        mockMvc.perform(get("/api/history/42"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message", containsString("permission")));
    }
}
