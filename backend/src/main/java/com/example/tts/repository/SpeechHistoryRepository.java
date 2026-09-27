package com.example.tts.repository;

import com.example.tts.model.SpeechHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for SpeechHistory entity with User isolation support.
 */
@Repository
public interface SpeechHistoryRepository extends JpaRepository<SpeechHistory, Long> {

    /**
     * Retrieve all history entries ordered newest first
     */
    List<SpeechHistory> findAllByOrderByCreatedAtDesc();

    /**
     * Retrieve all history entries belonging to a specific user ordered newest first.
     * Uses JOIN FETCH on user association to eliminate N+1 query overhead.
     */
    @org.springframework.data.jpa.repository.Query("SELECT h FROM SpeechHistory h LEFT JOIN FETCH h.user WHERE h.user.id = :userId ORDER BY h.createdAt DESC")
    List<SpeechHistory> findAllByUserIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("userId") Long userId);

    /**
     * Retrieve a specific history entry only if it belongs to the given user
     */
    Optional<SpeechHistory> findByIdAndUserId(Long id, Long userId);

    /**
     * Check if a specific history entry exists for the given user
     */
    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * Delete a specific history entry belonging to the given user
     */
    void deleteByIdAndUserId(Long id, Long userId);

    /**
     * Retrieve top 50 history entries ordered newest first
     */
    List<SpeechHistory> findTop50ByOrderByCreatedAtDesc();

    /**
     * Find history entries matching a specific language
     */
    List<SpeechHistory> findByLanguageOrderByCreatedAtDesc(String language);
}
