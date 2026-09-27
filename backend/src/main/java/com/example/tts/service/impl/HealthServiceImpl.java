package com.example.tts.service.impl;

import com.example.tts.dto.HealthStatusDto;
import com.example.tts.service.HealthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;

@Service
public class HealthServiceImpl implements HealthService {

    @Value("${spring.application.name:text-to-speech}")
    private String applicationName;

    @Value("${spring.profiles.active:${spring.profiles.default:development}}")
    private String activeEnvironment;

    @Override
    public HealthStatusDto getSystemHealth() {
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        long uptimeSeconds = uptimeMillis / 1000;

        return HealthStatusDto.builder()
                .status("UP")
                .service(applicationName)
                .version("1.0.0")
                .environment(activeEnvironment)
                .javaVersion(System.getProperty("java.version"))
                .uptimeSeconds(uptimeSeconds)
                .serverTime(LocalDateTime.now())
                .build();
    }
}
