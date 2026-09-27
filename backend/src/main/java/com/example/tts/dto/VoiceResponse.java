package com.example.tts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceResponse {

    private String id;
    private String name;
    private String language;
    private String gender;

    @Builder.Default
    private List<String> supportedStyles = List.of("default");

    public VoiceResponse(String id, String name, String language, String gender) {
        this.id = id;
        this.name = name;
        this.language = language;
        this.gender = gender;
        this.supportedStyles = List.of("default");
    }
}

