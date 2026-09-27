package com.example.tts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatusDto {

    private String status;
    private String service;
    private String version;
    private String environment;
    private String javaVersion;
    private long uptimeSeconds;
    private LocalDateTime serverTime;
}
