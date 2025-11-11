import { api, setAccessToken } from '@/shared/services/api';
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
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
  async register(data: RegisterRequest): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>(
      '/auth/register',
      data,
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

  /**
   * Request password reset (forgot password)
   * Sends a reset link to the user's email
   */
  async forgotPassword(data: ForgotPasswordRequest): Promise<void> {
    await api.post('/auth/forgot-password', data);
  },

  /**
   * Reset password using token
   */
  async resetPassword(data: ResetPasswordRequest): Promise<void> {
    await api.post('/auth/reset-password', data);
  },
};
