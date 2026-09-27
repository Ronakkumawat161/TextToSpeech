import apiClient from './api';

/**
 * Service communicating with Spring Boot AI text enhancement endpoint.
 * Keeps API keys and model credentials strictly isolated on backend.
 */
export const aiService = {
  /**
   * Enhance input text with AI (SUMMARIZE, CORRECT_GRAMMAR, REWRITE, CONVERSATIONAL).
   * POST /api/ai/enhance
   *
   * @param {Object} params
   * @param {string} params.text - The text to enhance
   * @param {'SUMMARIZE'|'CORRECT_GRAMMAR'|'REWRITE'|'CONVERSATIONAL'} params.operation - The enhancement operation
   * @returns {Promise<{
   *   success: boolean,
   *   originalText: string,
   *   enhancedText: string,
   *   operation: string,
   *   provider: string
   * }>}
   */
  async enhanceText({ text, operation }) {
    if (!text || text.trim().length === 0) {
      throw {
        userFriendlyMessage: 'Please enter or upload some text before requesting AI enhancement.'
      };
    }

    const response = await apiClient.post('/api/ai/enhance', {
      text: text.trim(),
      operation
    });

    return response.data;
  }
};

export default aiService;
