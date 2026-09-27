package com.example.tts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioSynthesisResult {

    private byte[] audioBytes;
    private String format;          // "audio/mpeg"
    private String filename;        // e.g. "speech-en-female-1.mp3"
    private String language;
    private String voice;
    private int characterCount;
    private String provider;
}
