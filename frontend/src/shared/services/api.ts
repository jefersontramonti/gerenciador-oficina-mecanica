import axios, { AxiosError } from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';
import { env } from '@/config/env';
import type { ApiError, ApiResponse } from '../types/api';

/**
 * Axios instance with interceptors for JWT authentication
 */
export const api = axios.create({
  baseURL: env.apiUrl,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds
});

// Token management with localStorage persistence
const TOKEN_STORAGE_KEY = 'pitstop_access_token';

let accessToken: string | null = null;

// Load token from localStorage on app init
const loadTokenFromStorage = (): string | null => {
  try {
    return localStorage.getItem(TOKEN_STORAGE_KEY);
  } catch (error) {
    console.error('Error loading token from localStorage:', error);
    return null;
  }
};

// Initialize token from storage
accessToken = loadTokenFromStorage();

export const setAccessToken = (token: string | null) => {
  accessToken = token;

  // Persist to localStorage
  try {
    if (token) {
      localStorage.setItem(TOKEN_STORAGE_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
    }
  } catch (error) {
    console.error('Error saving token to localStorage:', error);
  }
};

export const getAccessToken = () => accessToken;

/**
 * Request interceptor - Add JWT token to requests
 */
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken();

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Response interceptor - Handle token refresh and errors
 */
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: any) => void;
}> = [];

const processQueue = (error: Error | null, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });

  failedQueue = [];
};

api.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error: AxiosError<ApiError>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    // Handle 401 Unauthorized - Token expired
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Queue the request while token is being refreshed
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`;
            }
            return api(originalRequest);
          })
          .catch((err) => {
            return Promise.reject(err);
          });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Try to refresh token
        const refreshResponse = await api.post<{ accessToken: string }>(
          '/auth/refresh',
          {},
          {
            withCredentials: true, // Send refresh token cookie
          }
        );

        const newToken = refreshResponse.data.accessToken;
        setAccessToken(newToken);

        // Update the failed request with new token
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
        }

        processQueue(null, newToken);

        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError as Error, null);

        // Clear token and redirect to login
        setAccessToken(null);
        window.location.href = '/login';

        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // Handle other errors
    const apiError: ApiError = error.response?.data || {
      error: 'NETWORK_ERROR',
      message: error.message || 'Erro de conexão com o servidor',
      status: error.response?.status || 0,
      timestamp: new Date().toISOString(),
      path: originalRequest?.url || '',
    };

    return Promise.reject(apiError);
  }
);

/**
 * Helper functions for common API operations
 */
export const apiHelpers = {
  /**
   * Extract data from API response
   */
  extractData: <T>(response: ApiResponse<T>): T => {
    return response.data;
  },

  /**
   * Build query string from params
   */
  buildQueryString: (params: Record<string, any>): string => {
    const query = new URLSearchParams();

    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        query.append(key, String(value));
      }
    });

    return query.toString();
  },

  /**
   * Handle API error and extract message
   */
  getErrorMessage: (error: unknown): string => {
    if (axios.isAxiosError(error)) {
      const apiError = error.response?.data as ApiError;
      return apiError?.message || 'Erro desconhecido';
    }

    if (error instanceof Error) {
      return error.message;
    }

    return 'Erro desconhecido';
  },

  /**
   * Check if error is validation error
   */
  isValidationError: (error: unknown): error is ApiError & { validationErrors: Record<string, string[]> } => {
    if (axios.isAxiosError(error)) {
      const apiError = error.response?.data as ApiError;
      return !!apiError?.validationErrors;
    }
    return false;
  },
};
