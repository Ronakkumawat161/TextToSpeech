package com.example.tts.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /**
     * Provider mode: "MOCK", "OPENAI", "GEMINI", or "EXTERNAL"
     */
    private String provider = "MOCK";

    /**
     * Secret API key for external AI provider (kept strictly on server side)
     */
    private String apiKey = "";

    /**
     * Target AI chat completion REST endpoint
     */
    private String endpoint = "https://api.openai.com/v1/chat/completions";

    /**
     * AI model identifier
     */
    private String model = "gpt-4o-mini";

    /**
     * Connection timeout in milliseconds
     */
    private int connectTimeoutMs = 5000;

    /**
     * Socket read timeout in milliseconds
     */
    private int readTimeoutMs = 12000;
}
