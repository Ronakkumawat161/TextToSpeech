package com.example.tts.service.provider;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Mock TTS Provider implementation.
 * Supports MP3 as the primary format, generating standard MPEG-1 Layer III audio frames
 * that HTML5 audio players can natively decode, play, and seek.
 */
@Component("mockTtsProvider")
public class MockTtsProvider implements TtsProvider {

    private static final String PROVIDER_NAME = "MOCK";
    private static final String AUDIO_FORMAT = "audio/mpeg";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getAudioFormat() {
        return AUDIO_FORMAT;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public byte[] synthesize(String text, String language, String voice) {
        return synthesize(text, language, voice, 1.0, 0, 1.0, "default");
    }

    @Override
    public byte[] synthesize(String text, String language, String voice, Double speed, Integer pitch, Double volume, String style) {
        int length = text != null ? text.length() : 10;
        double safeSpeed = (speed != null && speed >= 0.25) ? speed : 1.0;
        double safeVolume = (volume != null) ? Math.min(1.0, Math.max(0.0, volume)) : 1.0;
        int safePitch = (pitch != null) ? pitch : 0;

        // Duration scales inversely with speaking speed (faster speech = shorter audio duration)
        double baseDuration = Math.max(1.5, Math.min(5.0, length * 0.05));
        double adjustedDuration = Math.max(0.5, baseDuration / safeSpeed);

        // At 44.1 kHz, 1 MPEG-1 Layer 3 frame = 1152 samples = ~26.12ms
        int frameCount = (int) Math.round((adjustedDuration * 44100.0) / 1152.0);

        return createMp3AudioBytes(Math.max(20, frameCount), safeVolume, safePitch);
    }

    /**
     * Generates standard MPEG-1 Audio Layer III (MP3) frames.
     * Bitrate: 128 kbps, Sample Rate: 44.1 kHz, Channel: Mono.
     * Frame length = floor(144 * 128000 / 44100) = 417 bytes.
     */
    private byte[] createMp3AudioBytes(int frameCount, double volume, int pitch) {
        int frameLength = 417;
        byte[] audioBytes = new byte[frameCount * frameLength];

        // Pitch shift multiplier: +10% pitch -> higher frequency multiplier
        double pitchMultiplier = 1.0 + (pitch / 100.0);
        double frequencyFactor = Math.max(0.2, Math.min(3.0, pitchMultiplier)) * 0.1;
        double amplitude = Math.max(0.0, Math.min(1.0, volume)) * 20.0;

        for (int i = 0; i < frameCount; i++) {
            int offset = i * frameLength;

            // Frame Header (4 bytes)
            // 0xFF, 0xFB: 11111111 11111011 (Sync word + MPEG-1 + Layer III + No CRC)
            audioBytes[offset] = (byte) 0xFF;
            audioBytes[offset + 1] = (byte) 0xFB;

            // 0x90: 10010000 (Bitrate 1001 = 128 kbps, Sample Rate 00 = 44100 Hz, Padding 0, Private 0)
            audioBytes[offset + 2] = (byte) 0x90;

            // 0x64: 01100100 (Channel Mode 11 = Mono / Single Channel, Mode Extension 00, Copyright 0, Original 1)
            audioBytes[offset + 3] = (byte) 0x64;

            // Side information for mono (17 bytes)
            int sideInfoOffset = offset + 4;
            audioBytes[sideInfoOffset] = (byte) 0x00;
            audioBytes[sideInfoOffset + 1] = (byte) 0x00;

            // Remaining main data payload scaled by volume and pitch
            for (int b = 21; b < frameLength; b++) {
                double phase = (double) (offset + b) / 44.1;
                audioBytes[offset + b] = (byte) ((Math.sin(phase * frequencyFactor) * amplitude) % 127);
            }
        }

        return audioBytes;
    }
}

