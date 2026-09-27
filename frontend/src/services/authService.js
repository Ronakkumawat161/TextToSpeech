import apiClient from './api';

const TOKEN_KEY = 'tts_auth_token';
const USER_KEY = 'tts_auth_user';

/**
 * Service handling JWT authentication, registration, token persistence, and session state.
 */
export const authService = {
  /**
   * Register a new user account
   */
  async register({ name, email, password }) {
    const response = await apiClient.post('/api/auth/register', {
      name: name.trim(),
      email: email.trim().toLowerCase(),
      password
    });

    if (response.data && response.data.token) {
      this.saveSession(response.data.token, response.data.user);
    }
    return response.data;
  },

  /**
   * Authenticate an existing user with credentials
   */
  async login({ email, password }) {
    const response = await apiClient.post('/api/auth/login', {
      email: email.trim().toLowerCase(),
      password
    });

    if (response.data && response.data.token) {
      this.saveSession(response.data.token, response.data.user);
    }
    return response.data;
  },

  /**
   * Retrieve currently authenticated user profile from backend
   */
  async getProfile() {
    const response = await apiClient.get('/api/auth/me');
    if (response.data) {
      localStorage.setItem(USER_KEY, JSON.stringify(response.data));
    }
    return response.data;
  },

  /**
   * Terminate user session and remove tokens
   */
  logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },

  /**
   * Store JWT token and user profile in localStorage
   */
  saveSession(token, user) {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    }
    if (user) {
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    }
  },

  /**
   * Retrieve current JWT token from storage
   */
  getToken() {
    return localStorage.getItem(TOKEN_KEY) || null;
  },

  /**
   * Retrieve cached user details from storage
   */
  getCurrentUser() {
    const userStr = localStorage.getItem(USER_KEY);
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch (e) {
      return null;
    }
  },

  /**
   * Check whether a valid JWT session is present
   */
  isAuthenticated() {
    return Boolean(this.getToken());
  }
};

export default authService;
