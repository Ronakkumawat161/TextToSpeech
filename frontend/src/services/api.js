import axios from 'axios';
import { API_BASE_URL } from '../utils/constants';

/**
 * Centralized Axios API client configured with environment-based baseURL
 * and response/error interceptors supporting both JSON and binary Blob responses.
 *
 * Implements Module 7 error decoding:
 * Maps 400, 401, 403, 404, 415, 429, 500, 503 status codes and standardized error responses:
 * { "success": false, "error": { "code": "...", "message": "...", "details": {...} } }
 */
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json, audio/mpeg, audio/*, */*'
  }
});

// Request Interceptor: Attach JWT Bearer token if present
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('tts_auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    console.error('[API Request Error]', error);
    return Promise.reject(error);
  }
);

// Response Interceptor
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    let userFriendlyMessage = 'An unexpected network error occurred.';
    let errorCode = null;
    let validationErrors = null;
    const status = error.response ? error.response.status : null;

    if (error.response) {
      let data = error.response.data;

      // When responseType was 'blob', decode the Blob to read backend ErrorResponse JSON
      if (typeof Blob !== 'undefined' && data instanceof Blob) {
        try {
          const text = await data.text();
          data = JSON.parse(text);
        } catch (e) {
          // Failed to parse blob as json; keep original
        }
      }

      // Extract error details from standardized backend format:
      // { success: false, error: { code, message, details } }
      errorCode = data?.error?.code || null;
      const backendMessage = data?.error?.message || data?.message;
      validationErrors = data?.error?.details || data?.validationErrors || null;

      if (backendMessage) {
        userFriendlyMessage = backendMessage;
      } else if (status === 400) {
        userFriendlyMessage = 'Invalid request parameters. Please verify input text, voice, and language.';
      } else if (status === 401) {
        userFriendlyMessage = 'Authentication required. You do not have valid credentials for this service.';
      } else if (status === 403) {
        userFriendlyMessage = 'Access forbidden. You do not have permission to access speech synthesis.';
      } else if (status === 404) {
        userFriendlyMessage = 'The requested speech synthesis endpoint or voice was not found.';
      } else if (status === 415) {
        userFriendlyMessage = 'Unsupported media type: request Content-Type must be application/json.';
      } else if (status === 429) {
        userFriendlyMessage = 'Rate limit exceeded: too many requests. Please wait a moment before trying again.';
      } else if (status === 503) {
        userFriendlyMessage = 'Text-to-speech service is temporarily unavailable. Please try again shortly.';
      } else if (status === 500) {
        userFriendlyMessage = 'An unexpected server error occurred while synthesizing speech. Please try again later.';
      } else {
        userFriendlyMessage = `Server returned error (${status}). Please try again.`;
      }
    } else if (error.code === 'ECONNABORTED' || (error.message && error.message.includes('timeout'))) {
      errorCode = 'TIMEOUT';
      userFriendlyMessage = 'Network timeout: speech synthesis request timed out while communicating with backend.';
    } else if (error.request) {
      errorCode = 'NETWORK_ERROR';
      userFriendlyMessage = `Network interruption: unable to reach backend server at ${API_BASE_URL}. Please verify Spring Boot is running on port 8080.`;
    } else {
      userFriendlyMessage = error.message || userFriendlyMessage;
    }

    console.error('[API Response Error]', {
      status,
      code: errorCode,
      message: userFriendlyMessage,
      validationErrors
    });

    return Promise.reject({
      ...error,
      status,
      errorCode,
      userFriendlyMessage,
      validationErrors
    });
  }
);

export default apiClient;
