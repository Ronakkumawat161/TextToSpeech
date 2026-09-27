package com.example.tts.controller;

import com.example.tts.dto.ApiResponse;
import com.example.tts.dto.HealthStatusDto;
import com.example.tts.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    /**
     * Standard module health endpoint
     * GET /api/health
     */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("message", "Text-to-Speech backend is running");
        return ResponseEntity.ok(response);
    }

    /**
     * Diagnostic telemetry health endpoint
     * GET /api/v1/health
     */
    @GetMapping("/api/v1/health")
    public ResponseEntity<ApiResponse<HealthStatusDto>> checkHealth() {
        HealthStatusDto status = healthService.getSystemHealth();
        return ResponseEntity.ok(ApiResponse.success(status, "System is healthy and operational"));
    }
}
