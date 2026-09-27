import apiClient from './api';

export const healthService = {
  /**
   * Fetch backend health and diagnostic details
   * @returns {Promise<{status: string, message: string, latencyMs: number}>}
   */
  async checkHealth() {
    const startTime = performance.now();
    const response = await apiClient.get('/api/health');
    const endTime = performance.now();
    const latencyMs = Math.round(endTime - startTime);

    return {
      ...response.data,
      latencyMs
    };
  }
};
