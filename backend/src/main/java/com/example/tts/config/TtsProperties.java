package com.example.tts.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.tts")
public class TtsProperties {

    /**
     * Provider mode: "MOCK", "AZURE", or "EXTERNAL"
     */
    private String provider = "MOCK";

    /**
     * Secret API key for external TTS provider (kept strictly on server side)
     */
    private String apiKey = "";

    /**
     * Cloud provider region (e.g., "eastus", "westus", "centralindia")
     */
    private String region = "eastus";

    /**
     * Target synthesis REST endpoint
     */
    private String endpoint = "";

    /**
     * Connection timeout in milliseconds
     */
    private int connectTimeoutMs = 5000;

    /**
     * Socket read timeout in milliseconds
     */
    private int readTimeoutMs = 10000;
}
