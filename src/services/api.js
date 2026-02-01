/**
 * Centralized API service for all backend communication
 */

import axios from 'axios';
import { API_ENDPOINTS, STORAGE_KEYS, TIMEOUTS } from '../config/constants';

// Create axios instance with default configuration
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: TIMEOUTS.DEFAULT,
  headers: {
    'Content-Type': 'application/json',
  },
  validateStatus: (status) => status >= 200 && status < 400,
});

// Request interceptor to add auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // Handle 401 Unauthorized - token expired or invalid
    if (error.response?.status === 401) {
      localStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN);
      // Optionally redirect to login
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

/**
 * Authentication API
 */
export const authAPI = {
  /**
   * Login with email and password
   * @param {Object} credentials - { email, password }
   * @returns {Promise} Response with token
   */
  login: async (credentials) => {
    const response = await apiClient.post(API_ENDPOINTS.LOGIN, credentials);
    return response.data;
  },

  /**
   * Register new user
   * @param {Object} userData - User registration data
   * @returns {Promise} Response data
   */
  register: async (userData) => {
    const response = await apiClient.post('/public/api/create-user', userData);
    return response.data;
  },

  /**
   * Get current user information
   * @returns {Promise} User info object
   */
  getUserInfo: async () => {
    const response = await apiClient.get(API_ENDPOINTS.GET_USER_INFO);
    return response.data;
  },

  /**
   * Handle OAuth callback
   * @param {string} provider - OAuth provider (google, github, linkedin)
   * @param {string} code - Authorization code from OAuth provider
   * @returns {Promise} Response with token
   */
  oauthCallback: async (provider, code) => {
    const endpoint = API_ENDPOINTS.OAUTH[provider.toUpperCase()];
    if (!endpoint) {
      throw new Error(`Unsupported OAuth provider: ${provider}`);
    }
    const response = await apiClient.get(endpoint, {
      params: { code },
      timeout: TIMEOUTS.OAUTH,
    });
    return response.data;
  },
};

/**
 * Events API
 */
export const eventsAPI = {
    /**
     * Like an event
     * @param {string|number} eventId - Event ID
     * @returns {Promise}
     */
    createLikeEvent: async (eventId) => {
      const response = await apiClient.post(`/api/interactions/${eventId}`);
      return response.data;
    },

    /**
     * Unlike an event
     * @param {string|number} eventId - Event ID
     * @returns {Promise}
     */
    deleteLikeEvent: async (eventId) => {
      const response = await apiClient.delete(`/api/interactions/${eventId}`);
      return response.data;
    },
  /**
   * Get events by filter criteria
   * @param {Object} filterPayload - Filter criteria
   * @returns {Promise} Array of events
   */
  getByFilter: async (filterPayload = {}) => {
    const response = await apiClient.post(API_ENDPOINTS.EVENTS.GET_BY_FILTER, filterPayload);
    return response.data;
  },

  /**
   * Get event by ID
   * @param {string|number} eventId - Event ID
   * @returns {Promise} Event object
   */
  getById: async (eventId) => {
    const response = await apiClient.get(API_ENDPOINTS.EVENTS.GET_BY_ID, {
      params: { eventId },
    });
    return response.data;
  },

  /**
   * Search events by name and organizer
   * @param {Object} params - { eventName, organizerName, isMyEventList, page, size }
   * @returns {Promise} Paginated response with events
   */
  getByNameAndOrganizer: async (params) => {
    const response = await apiClient.get(API_ENDPOINTS.EVENTS.GET_BY_NAME_AND_ORGANIZER, {
      params,
    });
    return response.data;
  },

  /**
   * Create a new event
   * @param {Object} eventData - Event data
   * @returns {Promise} Created event object
   */
  create: async (eventData) => {
    const response = await apiClient.post(API_ENDPOINTS.EVENTS.CREATE, eventData);
    return response.data;
  },

  /**
   * Update an existing event
   * @param {string|number} eventId - Event ID
   * @param {Object} eventData - Updated event data
   * @returns {Promise} Updated event object
   */
  update: async (eventId, eventData) => {
    const response = await apiClient.post(`${API_ENDPOINTS.EVENTS.UPDATE}/${eventId}`, eventData);
    return response.data;
  },

  /**
   * Schedule an event
   * @param {string|number} eventId - Event ID
   * @returns {Promise} Response data
   */
  schedule: async (eventId) => {
    const response = await apiClient.put(`/public/api/events/schedule/${eventId}`, {});
    return response.data;
  },

  /**
   * Cancel an event
   * @param {string|number} eventId - Event ID
   * @returns {Promise} Response data
   */
  cancel: async (eventId) => {
    const response = await apiClient.put(`/public/api/events/cancel/${eventId}`, {});
    return response.data;
  },

  /**
   * Get events organized by current user
   * @param {Object} params - { eventName, page, size }
   * @returns {Promise} Paginated response with events
   */
  getByOrganizer: async (params) => {
    const response = await apiClient.get('/public/api/events/getByNameOrganiserByMe', {
      params,
    });
    return response.data;
  },
  /**
   * Watchlist: Get all liked events (watchlist)
   * @returns {Promise} Array of liked events
   */
  getWatchlist: async () => {
    const response = await apiClient.get('/api/interactions');
    return response.data;
  },

  /**
   * Watchlist: Add event to watchlist
   * @param {string|number} eventId - Event ID
   * @returns {Promise} Response
   */
  addToWatchlist: async (eventId) => {
    const response = await apiClient.post(`/api/interactions/watchlist/${eventId}`);
    return response.data;
  },

  /**
   * Watchlist: Remove event from watchlist
   * @param {string|number} eventId - Event ID
   * @returns {Promise} Response
   */
  removeFromWatchlist: async (eventId) => {
    const response = await apiClient.delete(`/api/interactions/watchlist/${eventId}`);
    return response.data;
  },
};

/**
 * Registrations API
 */
export const registrationsAPI = {
  /**
   * Get events registered by current user
   * @returns {Promise} Array of registered events
   */
  getMyEvents: async () => {
    const response = await apiClient.get(API_ENDPOINTS.REGISTRATIONS.GET_MY_EVENTS);
    return response.data;
  },

  /**
   * Register for an event
   * @param {string|number} eventId - Event ID
   * @returns {Promise} Registration response
   */
  register: async (eventId) => {
    const response = await apiClient.post(`/api/registrations/register-event/${eventId}`, {});
    return response.data;
  },

  /**
   * Cancel event registration
   * @param {string|number} eventId - Event ID
   * @returns {Promise} Cancellation response
   */
  cancel: async (eventId) => {
    const response = await apiClient.delete(`/api/registrations/unregister-event/${eventId}`);
    return response.data;
  },
};

/**
 * Machine Learning API
 */
export const mlAPI = {
  /**
   * Predict event metrics
   * @param {string|number} eventId - Event ID
   * @returns {Promise} Prediction data
   */
  predictEvent: async (eventId) => {
    const response = await apiClient.get(`/api/ml/predict/event/${eventId}`, {
      validateStatus: (status) => status >= 200 && status < 500,
    });
    return response.data;
  },

  /**
   * Get latest prediction for an event
   * @param {string|number} eventId - Event ID
   * @returns {Promise} Latest prediction data
   */
  getLatestPrediction: async (eventId) => {
    const response = await apiClient.get(`/api/ml/prediction/latest/${eventId}`, {
      validateStatus: (status) => status >= 200 && status < 500,
    });
    return response.data;
  },

  /**
   * Get prediction history for an event
   * @param {string|number} eventId - Event ID
   * @returns {Promise} Prediction history data
   */
  getPredictionHistory: async (eventId) => {
    const response = await apiClient.get(`/api/ml/prediction/history/${eventId}`, {
      validateStatus: (status) => status >= 200 && status < 500,
    });
    return response.data;
  },

  /**
   * Check ML service health
   * @returns {Promise} Health check data
   */
  checkHealth: async () => {
    const response = await apiClient.get('/api/ml/health', {
      validateStatus: (status) => status >= 200 && status < 500,
    });
    return response.data;
  },

  /**
   * Get ML statistics
   * @returns {Promise} ML statistics data
   */
  getStats: async () => {
    const response = await apiClient.get('/api/ml/stats', {
      validateStatus: (status) => status >= 200 && status < 500,
    });
    return response.data;
  },
};

export default apiClient;

