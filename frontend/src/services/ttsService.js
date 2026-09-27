import apiClient from './api';

/**
 * Text-to-Speech Service communicating with Spring Boot REST API.
 * Implements Option A: Direct audio bytes delivery with audio/mpeg Content-Type.
 */
export const ttsService = {
  /**
   * Fetch available voices from backend
   * GET /api/voices
   *
   * @param {string} [language] - Optional language code to filter voices
   * @returns {Promise<Array<{id: string, name: string, language: string, gender: string}>>}
   */
  async getVoices(language) {
    const params = language ? { language } : {};
    const response = await apiClient.get('/api/voices', { params });
    return response.data;
  },

  /**
   * Synthesize text to speech returning binary audio stream (Option A).
   * POST /api/tts
   *
   * @param {Object} request
   * @param {string} request.text - Text to convert to speech
   * @param {string} request.language - Target language code (e.g. "en-US", "hi-IN")
   * @param {string} request.voice - Target voice identifier (e.g. "en-female-1")
   * @param {number} [request.speed=1.0] - Speaking speed multiplier (0.5 to 2.0)
   * @param {number} [request.pitch=0] - Pitch adjustment (-20 to 20)
   * @param {number} [request.volume=1.0] - Volume level (0.0 to 1.0)
   * @param {string} [request.style='default'] - Expressive voice style
   * @returns {Promise<{
   *   audioBlob: Blob,
   *   audioUrl: string,
   *   format: string,
   *   filename: string,
   *   voice: string,
   *   language: string,
   *   characterCount: number,
   *   provider: string
   * }>}
   */
  async generateSpeech({ 
    text, 
    language, 
    voice, 
    speed = 1.0, 
    pitch = 0, 
    volume = 1.0, 
    style = 'default' 
  }) {
    const response = await apiClient.post(
      '/api/tts',
      { text, language, voice, speed, pitch, volume, style },
      { responseType: 'blob' }
    );


    const blob = response.data;

    // Validation: Catch empty or corrupted audio responses
    if (!blob || !(blob instanceof Blob) || blob.size === 0) {
      throw {
        userFriendlyMessage: 'Received empty or corrupted audio response from speech synthesis provider.'
      };
    }

    // Extract filename from Content-Disposition header if present
    let filename = `speech-${voice}-${Date.now()}.mp3`;
    const disposition = response.headers ? response.headers['content-disposition'] : null;
    if (disposition && disposition.includes('filename=')) {
      const match = disposition.match(/filename="?([^";]+)"?/);
      if (match && match[1]) {
        filename = match[1];
      }
    }

    const format = (response.headers && response.headers['content-type']) || 'audio/mpeg';
    const characterCount = Number(response.headers && response.headers['x-character-count']) || text.length;
    const provider = (response.headers && response.headers['x-provider']) || 'TTS Provider';

    // Create browser Object URL from binary audio blob
    const audioUrl = URL.createObjectURL(blob);

    return {
      audioBlob: blob,
      audioUrl,
      format,
      filename,
      voice,
      language,
      characterCount,
      provider
    };
  },

  /**
   * Check backend health status
   * GET /api/health
   */
  async checkHealth() {
    const response = await apiClient.get('/api/health');
    return response.data;
  }
};

export default ttsService;
