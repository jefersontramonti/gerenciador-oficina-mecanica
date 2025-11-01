import { api, setAccessToken } from '@/shared/services/api';
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  Usuario,
} from '../types';

/**
 * Authentication service
 * Handles all auth-related API calls
 */
export const authService = {
  /**
   * Login user
   */
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>(
      '/auth/login',
      credentials,
      {
        withCredentials: true, // Important: Send/receive cookies for refresh token
      }
    );

    const loginData = response.data;

    // Store access token in memory
    setAccessToken(loginData.accessToken);

    return loginData;
  },

  /**
   * Register new user
   */
  async register(data: RegisterRequest): Promise<Usuario> {
    const response = await api.post<Usuario>('/auth/register', data);
    return response.data;
  },

  /**
   * Logout user
   */
  async logout(): Promise<void> {
    try {
      await api.post(
        '/auth/logout',
        {},
        {
          withCredentials: true, // Send refresh token cookie
        }
      );
    } finally {
      // Clear token even if request fails
      setAccessToken(null);
    }
  },

  /**
   * Refresh access token
   */
  async refreshToken(): Promise<string> {
    const response = await api.post<{ accessToken: string }>(
      '/auth/refresh',
      {},
      {
        withCredentials: true, // Send refresh token cookie
      }
    );

    const newToken = response.data.accessToken;
    setAccessToken(newToken);

    return newToken;
  },

  /**
   * Get current user profile
   */
  async getCurrentUser(): Promise<Usuario> {
    const response = await api.get<Usuario>('/auth/me');
    return response.data;
  },

  /**
   * Update user profile
   */
  async updateProfile(data: Partial<Usuario>): Promise<Usuario> {
    const response = await api.put<Usuario>('/auth/profile', data);
    return response.data;
  },

  /**
   * Change password
   */
  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await api.put('/auth/password', {
      currentPassword,
      newPassword,
    });
  },
};
