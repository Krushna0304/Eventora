/**
 * Application-wide constants
 */

// Storage keys
export const STORAGE_KEYS = {
  AUTH_TOKEN: 'eventora_token',
  OAUTH_PROCESSING: 'oauth_processing',
};

// API endpoints
export const API_ENDPOINTS = {
  // Public endpoints
  LOGIN: '/public/api/login',
  REGISTER: '/public/api/register',
  GET_USER_INFO: '/public/api/getUserInfo',
  EVENTS: {
    GET_BY_FILTER: '/public/api/events/getByFilter',
    GET_BY_ID: '/public/api/events/getById',
    GET_BY_NAME_AND_ORGANIZER: '/public/api/events/getByNameAndOrganizer',
    CREATE: '/public/api/events/create',
    UPDATE: '/public/api/events/update',
    RECOMMENDATIONS: '/api/recommendations',
  },
  // Protected endpoints
  REGISTRATIONS: {
    GET_MY_EVENTS: '/api/registrations/getMyEvents',
    REGISTER: '/api/registrations/register',
    CANCEL: '/api/registrations/cancel',
  },
  // OAuth endpoints
  OAUTH: {
    GOOGLE: '/auth/google/code',
    GITHUB: '/auth/github/code',
    LINKEDIN: '/auth/linkedin/code',
  },
};

// OAuth providers
export const OAUTH_PROVIDERS = {
  GOOGLE: 'google',
  GITHUB: 'github',
  LINKEDIN: 'linkedin',
};

// Application routes
export const ROUTES = {
  HOME: '/home',
  LOGIN: '/login',
  REGISTER: '/register',
  ORGANISER_DASHBOARD: '/organiser',
  CREATE_EVENT: '/create-event',
  EDIT_EVENT: '/edit-event/:id',
  EVENT_DETAIL: '/events/:id',
  ORGANISER_EVENT_DETAIL: '/organiser/events/:id',
};

// Default pagination
export const PAGINATION = {
  DEFAULT_PAGE: 0,
  DEFAULT_SIZE: 10,
};

// Request timeouts (in milliseconds)
export const TIMEOUTS = {
  DEFAULT: 10000,
  OAUTH: 10000,
};

