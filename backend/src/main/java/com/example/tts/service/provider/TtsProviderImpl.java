package com.example.tts.service.provider;

import com.example.tts.config.TtsProperties;
import com.example.tts.exception.InvalidVoiceException;
import com.example.tts.exception.TtsAuthenticationException;
import com.example.tts.exception.TtsProviderUnavailableException;
import com.example.tts.exception.TtsSynthesisException;
import com.example.tts.exception.TtsTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.Duration;

/**
 * Production-ready TTS Provider Implementation.
 * Encapsulates communication with external cloud speech synthesis APIs (e.g. Azure / REST).
 * 
 * Features:
 * - Proper connect and read timeout configurations.
 * - Comprehensive HTTP status error mapping (authentication, bad request, timeouts, provider downtime).
 * - Automatic delegation to MockTtsProvider when external credentials are not configured.
 * - Strictly isolates provider logic so controllers and service layer remain provider-agnostic.
 */
@Slf4j
@Primary
@Component("ttsProviderImpl")
public class TtsProviderImpl implements TtsProvider {

    private final TtsProperties properties;
    private final MockTtsProvider mockTtsProvider;
    private final RestTemplate restTemplate;

    public TtsProviderImpl(TtsProperties properties, MockTtsProvider mockTtsProvider) {
        this.properties = properties;
        this.mockTtsProvider = mockTtsProvider;

        // Configure RestTemplate with strict timeout constraints
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String getProviderName() {
        return isConfigured() ? properties.getProvider().toUpperCase() : mockTtsProvider.getProviderName();
    }

    @Override
    public String getAudioFormat() {
        return "audio/mpeg";
    }

    @Override
    public boolean isConfigured() {
        String key = properties.getApiKey();
        return key != null &&
                !key.isBlank() &&
                !key.equalsIgnoreCase("mock") &&
                !key.equalsIgnoreCase("your_tts_provider_api_key_here");
    }

    @Override
    public byte[] synthesize(String text, String language, String voice) {
        return synthesize(text, language, voice, 1.0, 0, 1.0, "default");
    }

    @Override
    public byte[] synthesize(String text, String language, String voice, Double speed, Integer pitch, Double volume, String style) {
        // If credentials are unavailable or provider is explicitly set to MOCK, use structured mock provider
        if (!isConfigured() || "MOCK".equalsIgnoreCase(properties.getProvider())) {
            log.info("[TtsProviderImpl] External credentials not active (provider={}). Delegating to isolated MockTtsProvider.",
                    properties.getProvider());
            return mockTtsProvider.synthesize(text, language, voice, speed, pitch, volume, style);
        }

        // Determine destination endpoint
        String endpoint = determineEndpoint();
        log.info("[TtsProviderImpl] Invoking external speech synthesis API at '{}' (region='{}', speed={}, pitch={}, volume={}, style='{}')",
                endpoint, properties.getRegion(), speed, pitch, volume, style);

        String ssmlPayload = buildSsmlPayload(text, language, voice, speed, pitch, volume, style);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/ssml+xml"));
        headers.set("Ocp-Apim-Subscription-Key", properties.getApiKey());
        headers.set("X-Microsoft-OutputFormat", "audio-16khz-128kbitrate-mono-mp3");
        headers.set("User-Agent", "VoxFlow-TextToSpeech/1.0");

        HttpEntity<String> requestEntity = new HttpEntity<>(ssmlPayload, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );

            byte[] audioBytes = response.getBody();
            if (audioBytes == null || audioBytes.length == 0) {
                log.error("[TtsProviderImpl] Provider returned HTTP 200 but audio payload was empty");
                throw new TtsSynthesisException("External TTS provider returned empty audio content");
            }

            log.info("[TtsProviderImpl] Synthesis successful. Received {} audio bytes.", audioBytes.length);
            return audioBytes;

        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            log.error("[TtsProviderImpl] Authentication failure ({}): {}", ex.getStatusCode(), ex.getMessage());
            throw new TtsAuthenticationException("Invalid or unauthorized TTS API key or credentials. Verify TTS_API_KEY.");

        } catch (HttpClientErrorException.BadRequest ex) {
            String errorBody = ex.getResponseBodyAsString();
            log.warn("[TtsProviderImpl] Bad Request (400) from provider: {}", errorBody);

            // Graceful Fallback: If the provider rejects voice style or prosody for this voice, retry with standard synthesis
            boolean hasCustomStyle = style != null && !style.isBlank() && !"default".equalsIgnoreCase(style);
            boolean hasCustomProsody = (speed != null && Math.abs(speed - 1.0) > 0.01) ||
                                       (pitch != null && pitch != 0) ||
                                       (volume != null && Math.abs(volume - 1.0) > 0.01);

            if (hasCustomStyle && (errorBody.toLowerCase().contains("style") || errorBody.toLowerCase().contains("express-as"))) {
                log.info("[TtsProviderImpl] Voice '{}' does not support style '{}'. Gracefully falling back to default style.", voice, style);
                try {
                    String fallbackSsml = buildSsmlPayload(text, language, voice, speed, pitch, volume, "default");
                    HttpEntity<String> fallbackEntity = new HttpEntity<>(fallbackSsml, headers);
                    ResponseEntity<byte[]> fallbackResponse = restTemplate.exchange(endpoint, HttpMethod.POST, fallbackEntity, byte[].class);
                    if (fallbackResponse.getBody() != null && fallbackResponse.getBody().length > 0) {
                        return fallbackResponse.getBody();
                    }
                } catch (Exception fallbackEx) {
                    log.warn("[TtsProviderImpl] Fallback synthesis without style also failed: {}", fallbackEx.getMessage());
                }
            }

            if ((hasCustomStyle || hasCustomProsody) && (errorBody.toLowerCase().contains("prosody") || errorBody.toLowerCase().contains("rate") || errorBody.toLowerCase().contains("pitch") || errorBody.toLowerCase().contains("volume"))) {
                log.info("[TtsProviderImpl] Voice '{}' does not support custom prosody. Gracefully falling back to standard synthesis.", voice);
                try {
                    String fallbackSsml = buildSsmlPayload(text, language, voice, 1.0, 0, 1.0, "default");
                    HttpEntity<String> fallbackEntity = new HttpEntity<>(fallbackSsml, headers);
                    ResponseEntity<byte[]> fallbackResponse = restTemplate.exchange(endpoint, HttpMethod.POST, fallbackEntity, byte[].class);
                    if (fallbackResponse.getBody() != null && fallbackResponse.getBody().length > 0) {
                        return fallbackResponse.getBody();
                    }
                } catch (Exception fallbackEx) {
                    log.warn("[TtsProviderImpl] Fallback synthesis without prosody also failed: {}", fallbackEx.getMessage());
                }
            }

            if (errorBody.toLowerCase().contains("voice")) {
                throw new InvalidVoiceException("External TTS voice '" + voice + "' rejected by provider: " + errorBody);
            }
            throw new TtsSynthesisException("TTS provider rejected synthesis request: " + errorBody);

        } catch (HttpClientErrorException.NotFound ex) {
            log.error("[TtsProviderImpl] Endpoint or voice not found: {}", ex.getMessage());
            throw new InvalidVoiceException("TTS voice or endpoint not found on provider: " + voice);

        } catch (HttpServerErrorException ex) {
            log.error("[TtsProviderImpl] Upstream server error ({}): {}", ex.getStatusCode(), ex.getMessage());
            throw new TtsProviderUnavailableException("External TTS provider is temporarily unavailable: " + ex.getStatusText());

        } catch (ResourceAccessException ex) {
            if (ex.getCause() instanceof SocketTimeoutException) {
                log.error("[TtsProviderImpl] Request timed out after {}ms: {}", properties.getReadTimeoutMs(), ex.getMessage());
                throw new TtsTimeoutException("External TTS provider timed out after " + properties.getReadTimeoutMs() + "ms");
            }
            log.error("[TtsProviderImpl] Network connection failed: {}", ex.getMessage());
            throw new TtsProviderUnavailableException("Unable to establish network connection to external TTS provider: " + ex.getMessage());

        } catch (Exception ex) {
            log.error("[TtsProviderImpl] Unexpected provider synthesis failure", ex);
            throw new TtsSynthesisException("Failed to synthesize speech via external provider: " + ex.getMessage(), ex);
        }
    }

    private String determineEndpoint() {
        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            return properties.getEndpoint();
        }
        return "https://" + properties.getRegion() + ".tts.speech.microsoft.com/cognitiveservices/v1";
    }

    private String buildSsmlPayload(String text, String language, String voice, Double speed, Integer pitch, Double volume, String style) {
        String escaped = escapeXml(text);

        // Normalize prosody parameters
        double safeSpeed = (speed != null && speed >= 0.25) ? speed : 1.0;
        int safePitch = (pitch != null) ? pitch : 0;
        double safeVolume = (volume != null && volume >= 0.0) ? Math.min(1.0, volume) : 1.0;

        // Rate: e.g. "+15%" or "-20%"
        int ratePct = (int) Math.round((safeSpeed - 1.0) * 100);
        String rateAttr = (ratePct >= 0 ? "+" : "") + ratePct + "%";

        // Pitch: e.g. "+5%" or "-10%"
        String pitchAttr = (safePitch >= 0 ? "+" : "") + safePitch + "%";

        // Volume: e.g. "80%" or "100%"
        int volPct = (int) Math.round(safeVolume * 100);
        String volAttr = volPct + "%";

        String innerContent = String.format(java.util.Locale.ROOT,
                "<prosody rate='%s' pitch='%s' volume='%s'>%s</prosody>",
                rateAttr, pitchAttr, volAttr, escaped);

        // Apply style if non-default
        boolean hasCustomStyle = style != null && !style.isBlank() && !"default".equalsIgnoreCase(style) && !"none".equalsIgnoreCase(style);
        if (hasCustomStyle) {
            innerContent = String.format("<mstts:express-as style='%s'>%s</mstts:express-as>", escapeXml(style.trim().toLowerCase()), innerContent);
        }

        return "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xmlns:mstts='https://www.w3.org/2001/mstts' xml:lang='" + language + "'>" +
                "<voice xml:lang='" + language + "' name='" + voice + "'>" +
                innerContent +
                "</voice>" +
                "</speak>";
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

