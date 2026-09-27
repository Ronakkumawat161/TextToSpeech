-- ==========================================================
-- TEXT-TO-SPEECH (TTS) POSTGRESQL DATABASE SCHEMA MIGRATION
-- Tables: users, speech_history
-- ==========================================================

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

-- 2. SPEECH HISTORY TABLE
CREATE TABLE IF NOT EXISTS speech_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    language VARCHAR(20) NOT NULL,
    voice VARCHAR(100) NOT NULL,
    audio_url VARCHAR(500),
    storage_reference VARCHAR(500),
    character_count INTEGER,
    speed NUMERIC(3,2) DEFAULT 1.0,
    pitch INTEGER DEFAULT 0,
    volume NUMERIC(3,2) DEFAULT 1.0,
    style VARCHAR(50) DEFAULT 'default',
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- Performance & Retrieval Indexes
CREATE INDEX IF NOT EXISTS idx_speech_history_created_at ON speech_history (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_speech_history_lang_voice ON speech_history (language, voice);
CREATE INDEX IF NOT EXISTS idx_speech_history_user_id ON speech_history (user_id);
