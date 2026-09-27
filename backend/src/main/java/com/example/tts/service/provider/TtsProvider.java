package com.example.tts.service.provider;

/**
 * Strategy interface isolating speech synthesis provider implementations.
 * Keeps controllers and business services decoupled from external cloud providers.
 */
public interface TtsProvider {

    /**
     * Synthesize text to raw audio bytes (PCM/WAV/MP3).
     *
     * @param text     The input text to convert into speech
     * @param language The language code (e.g. "en-US", "hi-IN")
     * @param voice    The selected voice identifier
     * @return raw audio byte array
     */
    default byte[] synthesize(String text, String language, String voice) {
        return synthesize(text, language, voice, 1.0, 0, 1.0, "default");
    }

    /**
     * Synthesize text to raw audio bytes with configurable audio settings.
     *
     * @param text     The input text to convert into speech
     * @param language The language code (e.g. "en-US", "hi-IN")
     * @param voice    The selected voice identifier
     * @param speed    Speaking rate multiplier (e.g. 1.0)
     * @param pitch    Pitch adjustment in percentage (-100 to +100)
     * @param volume   Volume scale (0.0 to 1.0)
     * @param style    Voice style (e.g. "default", "cheerful", "calm")
     * @return raw audio byte array
     */
    byte[] synthesize(String text, String language, String voice, Double speed, Integer pitch, Double volume, String style);

    /**
     * Unique name of the active provider (e.g., "AZURE", "EXTERNAL", "MOCK")
     */
    String getProviderName();

    /**
     * Audio format produced by this provider (e.g., "audio/wav", "audio/mpeg")
     */
    String getAudioFormat();

    /**
     * Whether real external provider credentials are valid and active
     */
    boolean isConfigured();

    /**
     * Whether this provider supports speaking speed rate customization.
     */
    default boolean supportsSpeed() {
        return true;
    }

    /**
     * Whether this provider supports pitch customization.
     */
    default boolean supportsPitch() {
        return true;
    }

    /**
     * Whether this provider supports volume customization.
     */
    default boolean supportsVolume() {
        return true;
    }

    /**
     * Whether this provider supports expressive voice styles.
     */
    default boolean supportsStyles() {
        return true;
    }
}

