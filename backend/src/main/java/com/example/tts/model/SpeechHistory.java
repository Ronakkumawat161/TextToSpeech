package com.example.tts.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing synthesized speech history metadata.
 * Large binary audio data is stored externally / referenced by URL, keeping PostgreSQL lean.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "speech_history",
        indexes = {
                @Index(name = "idx_speech_history_created_at", columnList = "created_at DESC"),
                @Index(name = "idx_speech_history_lang_voice", columnList = "language, voice"),
                @Index(name = "idx_speech_history_user_id", columnList = "user_id")
        }
)
public class SpeechHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false, length = 20)
    private String language;

    @Column(nullable = false, length = 100)
    private String voice;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "storage_reference", length = 500)
    private String storageReference;

    @Column(name = "character_count")
    private Integer characterCount;

    @Column(name = "speed")
    @Builder.Default
    private Double speed = 1.0;

    @Column(name = "pitch")
    @Builder.Default
    private Integer pitch = 0;

    @Column(name = "volume")
    @Builder.Default
    private Double volume = 1.0;

    @Column(name = "style", length = 50)
    @Builder.Default
    private String style = "default";

    @Column(name = "is_favorite", nullable = false)
    @Builder.Default
    private Boolean favorite = false;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
