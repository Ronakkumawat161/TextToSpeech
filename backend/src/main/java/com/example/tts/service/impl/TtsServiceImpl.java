package com.example.tts.service.impl;

import com.example.tts.dto.AudioSynthesisResult;
import com.example.tts.dto.TtsRequest;
import com.example.tts.dto.TtsResponse;
import com.example.tts.dto.VoiceResponse;
import com.example.tts.exception.InvalidLanguageException;
import com.example.tts.exception.InvalidVoiceException;
import com.example.tts.service.TtsService;
import com.example.tts.service.provider.TtsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtsServiceImpl implements TtsService {

    private final TtsProvider ttsProvider;
    private final com.example.tts.service.SpeechHistoryService speechHistoryService;

    // Static catalog of available voices matching supported languages with configured style support
    private static final List<VoiceResponse> VOICE_CATALOG = List.of(
            new VoiceResponse("en-female-1", "English Female", "en-US", "FEMALE", List.of("default", "cheerful", "empathetic", "excited", "calm", "sad", "friendly")),
            new VoiceResponse("en-male-1", "English Male", "en-US", "MALE", List.of("default", "cheerful", "calm", "serious", "friendly", "whispering")),
            new VoiceResponse("hi-female-1", "Hindi Female", "hi-IN", "FEMALE", List.of("default", "cheerful", "calm")),
            new VoiceResponse("hi-male-1", "Hindi Male", "hi-IN", "MALE", List.of("default", "cheerful", "calm")),
            new VoiceResponse("gu-female-1", "Gujarati Female", "gu-IN", "FEMALE", List.of("default")),
            new VoiceResponse("gu-male-1", "Gujarati Male", "gu-IN", "MALE", List.of("default")),
            new VoiceResponse("mr-female-1", "Marathi Female", "mr-IN", "FEMALE", List.of("default")),
            new VoiceResponse("mr-male-1", "Marathi Male", "mr-IN", "MALE", List.of("default")),
            new VoiceResponse("es-female-1", "Spanish Female", "es-ES", "FEMALE", List.of("default", "cheerful", "calm")),
            new VoiceResponse("es-male-1", "Spanish Male", "es-ES", "MALE", List.of("default", "cheerful", "calm")),
            new VoiceResponse("fr-female-1", "French Female", "fr-FR", "FEMALE", List.of("default", "cheerful", "calm")),
            new VoiceResponse("fr-male-1", "French Male", "fr-FR", "MALE", List.of("default", "cheerful", "calm")),
            new VoiceResponse("de-female-1", "German Female", "de-DE", "FEMALE", List.of("default")),
            new VoiceResponse("de-male-1", "German Male", "de-DE", "MALE", List.of("default"))
    );

    @Override
    public List<VoiceResponse> getAvailableVoices() {
        return VOICE_CATALOG;
    }

    @Override
    public List<VoiceResponse> getVoicesByLanguage(String language) {
        if (language == null || language.isBlank()) {
            return Collections.emptyList();
        }

        String search = language.trim().toLowerCase();
        return VOICE_CATALOG.stream()
                .filter(v -> v.getLanguage().equalsIgnoreCase(search)
                        || v.getLanguage().toLowerCase().startsWith(search)
                        || search.startsWith(v.getLanguage().substring(0, 2).toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public AudioSynthesisResult synthesizeAudio(TtsRequest request) {
        // 1. Validate Request Body
        if (request == null) {
            throw new com.example.tts.exception.InvalidRequestException("Request body cannot be null");
        }

        // 2. Validate Text: empty, whitespace-only, and max length
        if (request.getText() == null || request.getText().isEmpty()) {
            throw new com.example.tts.exception.InvalidRequestException("Text is required and cannot be empty");
        }
        if (request.getText().trim().isEmpty()) {
            throw new com.example.tts.exception.InvalidRequestException("Text cannot consist of whitespace only");
        }
        if (request.getText().length() > 5000) {
            throw new com.example.tts.exception.InvalidRequestException("Text cannot exceed 5000 characters");
        }

        // 3. Validate Language: presence and catalog support
        if (request.getLanguage() == null || request.getLanguage().isBlank()) {
            throw new InvalidLanguageException("Language is required");
        }
        boolean isLanguageSupported = VOICE_CATALOG.stream()
                .anyMatch(v -> matchesLanguage(v.getLanguage(), request.getLanguage()));
        if (!isLanguageSupported) {
            log.warn("Synthesis rejected: invalid or unsupported language '{}'", request.getLanguage());
            throw new InvalidLanguageException(request.getLanguage());
        }

        // 4. Validate Voice: presence and catalog existence
        if (request.getVoice() == null || request.getVoice().isBlank()) {
            throw new InvalidVoiceException("Voice is required");
        }
        VoiceResponse selectedVoice = VOICE_CATALOG.stream()
                .filter(v -> v.getId().equalsIgnoreCase(request.getVoice().trim()))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Synthesis rejected: voice '{}' not found in catalog", request.getVoice());
                    return new InvalidVoiceException(request.getVoice());
                });

        // 5. Validate Voice-Language Compatibility
        if (!matchesLanguage(selectedVoice.getLanguage(), request.getLanguage())) {
            log.warn("Synthesis rejected: voice '{}' (language {}) incompatible with requested language '{}'",
                    selectedVoice.getId(), selectedVoice.getLanguage(), request.getLanguage());
            throw new com.example.tts.exception.VoiceLanguageMismatchException(selectedVoice.getId(), request.getLanguage());
        }

        // 6. Validate and Normalize Audio Customization Settings
        Double speed = request.getSpeed() != null ? request.getSpeed() : 1.0;
        if (speed < 0.25 || speed > 4.0) {
            throw new com.example.tts.exception.InvalidRequestException("Speaking speed must be between 0.25x and 4.0x. Received: " + speed);
        }

        Integer pitch = request.getPitch() != null ? request.getPitch() : 0;
        if (pitch < -100 || pitch > 100) {
            throw new com.example.tts.exception.InvalidRequestException("Pitch must be between -100% and +100%. Received: " + pitch);
        }

        Double volume = request.getVolume() != null ? request.getVolume() : 1.0;
        if (volume < 0.0 || volume > 1.0) {
            throw new com.example.tts.exception.InvalidRequestException("Volume must be between 0.0 and 1.0. Received: " + volume);
        }

        // 7. Validate Voice Style with Graceful Fallback
        String rawStyle = (request.getStyle() != null && !request.getStyle().isBlank())
                ? request.getStyle().trim().toLowerCase()
                : "default";
        String resolvedStyle = rawStyle;
        List<String> supportedStyles = selectedVoice.getSupportedStyles() != null ? selectedVoice.getSupportedStyles() : List.of("default");

        if (!"default".equals(rawStyle) && !supportedStyles.contains(rawStyle)) {
            log.info("Voice '{}' does not support style '{}'. Gracefully falling back to 'default' style.", selectedVoice.getId(), rawStyle);
            resolvedStyle = "default";
        }

        // Gracefully adapt settings based on provider capabilities
        Double effectiveSpeed = ttsProvider.supportsSpeed() ? speed : 1.0;
        Integer effectivePitch = ttsProvider.supportsPitch() ? pitch : 0;
        Double effectiveVolume = ttsProvider.supportsVolume() ? volume : 1.0;
        String effectiveStyle = ttsProvider.supportsStyles() ? resolvedStyle : "default";

        log.info("Processing binary audio synthesis: voice='{}', language='{}', speed={}, pitch={}, volume={}, style='{}', textLength={}",
                selectedVoice.getId(), selectedVoice.getLanguage(), effectiveSpeed, effectivePitch, effectiveVolume, effectiveStyle, request.getText().length());

        // 8. Delegate audio generation to isolated TTS provider
        byte[] audioBytes = ttsProvider.synthesize(
                request.getText(),
                selectedVoice.getLanguage(),
                selectedVoice.getId(),
                effectiveSpeed,
                effectivePitch,
                effectiveVolume,
                effectiveStyle
        );

        String filename = "speech-" + selectedVoice.getId() + "-" + System.currentTimeMillis() + ".mp3";

        // 9. Persist speech history metadata (keeping audio binaries out of PostgreSQL)
        try {
            speechHistoryService.saveHistory(
                    request.getText(),
                    selectedVoice.getLanguage(),
                    selectedVoice.getId(),
                    "/api/tts/audio/" + filename,
                    filename,
                    request.getText().length(),
                    speed,
                    pitch,
                    volume,
                    resolvedStyle
            );
        } catch (Exception ex) {
            log.warn("Database unavailable during speech history save: {}. Audio synthesis succeeded.", ex.getMessage());
        }

        return AudioSynthesisResult.builder()
                .audioBytes(audioBytes)
                .format(ttsProvider.getAudioFormat())
                .filename(filename)
                .language(selectedVoice.getLanguage())
                .voice(selectedVoice.getId())
                .characterCount(request.getText().length())
                .provider(ttsProvider.getProviderName())

                .build();
    }

    @Override
    public TtsResponse synthesize(TtsRequest request) {
        AudioSynthesisResult result = synthesizeAudio(request);
        String base64Audio = Base64.getEncoder().encodeToString(result.getAudioBytes());
        String dataUrl = "data:" + result.getFormat() + ";base64," + base64Audio;

        return TtsResponse.builder()
                .audioUrl(dataUrl)
                .audioBase64(base64Audio)
                .format(result.getFormat())
                .text(request.getText())
                .language(result.getLanguage())
                .voice(result.getVoice())
                .characterCount(result.getCharacterCount())
                .message("Speech synthesized successfully using " + result.getProvider() + " provider")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private boolean matchesLanguage(String catalogLang, String inputLang) {
        if (catalogLang == null || inputLang == null) return false;
        String cat = catalogLang.trim().toLowerCase();
        String inp = inputLang.trim().toLowerCase();

        return cat.equalsIgnoreCase(inp)
                || cat.startsWith(inp)
                || inp.startsWith(cat.substring(0, Math.min(2, cat.length())));
    }
}
