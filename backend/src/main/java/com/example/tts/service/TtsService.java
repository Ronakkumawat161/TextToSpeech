package com.example.tts.service;

import com.example.tts.dto.AudioSynthesisResult;
import com.example.tts.dto.TtsRequest;
import com.example.tts.dto.TtsResponse;
import com.example.tts.dto.VoiceResponse;

import java.util.List;

public interface TtsService {

    /**
     * Synthesize speech audio returning raw binary bytes and stream metadata.
     *
     * @param request valid TTS request parameters
     * @return AudioSynthesisResult with raw audio bytes, format, and filename
     */
    AudioSynthesisResult synthesizeAudio(TtsRequest request);

    /**
     * Synthesize speech audio returning JSON response envelope with Base64 payload.
     *
     * @param request valid TTS request parameters
     * @return TtsResponse containing audio data, format, and metadata
     */
    TtsResponse synthesize(TtsRequest request);

    /**
     * Retrieve all available voice configurations
     */
    List<VoiceResponse> getAvailableVoices();

    /**
     * Retrieve voices available for a specific language code
     */
    List<VoiceResponse> getVoicesByLanguage(String language);
}
