import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import { authService } from '../services/authService';
import type { AuthState, LoginRequest, Usuario } from '../types';

/**
 * SECURITY: User data persistence
 *
 * We store user profile data (nome, email, perfil) in localStorage for UX
 * when "Remember Me" is checked. This is safe because:
 * - User data is NOT sensitive (no passwords, tokens)
 * - It's just for UI convenience (show name, avatar)
 * - Access token is NEVER stored (kept in memory only)
 * - Refresh token is in HttpOnly cookie (backend managed)
 *
 * On app init, we try to refresh the access token using the HttpOnly cookie.
 */
const STORAGE_KEY_USER = 'pitstop_user';
const STORAGE_KEY_REMEMBER = 'pitstop_remember_me';

// Load user from localStorage if remember me is active
const loadUserFromStorage = (): Usuario | null => {
  try {
    const rememberMe = localStorage.getItem(STORAGE_KEY_REMEMBER);
    if (rememberMe === 'true') {
      const userJson = localStorage.getItem(STORAGE_KEY_USER);
      if (userJson) {
        return JSON.parse(userJson);
      }
    }
  } catch (error) {
    console.error('Error loading user from localStorage:', error);
  }
  return null;
};

// Save user to localStorage
const saveUserToStorage = (user: Usuario, rememberMe: boolean) => {
  try {
    if (rememberMe) {
      localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(user));
      localStorage.setItem(STORAGE_KEY_REMEMBER, 'true');
    } else {
      localStorage.removeItem(STORAGE_KEY_USER);
      localStorage.removeItem(STORAGE_KEY_REMEMBER);
    }
  } catch (error) {
    console.error('Error saving user to localStorage:', error);
  }
};

// Clear user from localStorage
const clearUserFromStorage = () => {
  try {
    localStorage.removeItem(STORAGE_KEY_USER);
    localStorage.removeItem(STORAGE_KEY_REMEMBER);
  } catch (error) {
    console.error('Error clearing user from localStorage:', error);
  }
};

const storedUser = loadUserFromStorage();

const initialState: AuthState = {
  user: storedUser,
  isAuthenticated: false, // Will be set to true after successful token refresh
  isLoading: true, // Always start with loading to attempt token refresh
  error: null,
};

/**
 * Async thunks for authentication
 */

/**
 * Initialize authentication on app startup
 *
 * SECURITY: Auto-authentication using refresh token
 * - Access token is in memory (lost on page refresh)
 * - Refresh token is in HttpOnly cookie (persists across refreshes)
 * - On app init, we attempt to get a new access token using the refresh token
 * - If successful, fetch user profile and restore session
 * - If failed, user needs to login again
 */
export const initializeAuth = createAsyncThunk(
  'auth/initialize',
  async (_, { rejectWithValue }) => {
    try {
      // Try to refresh access token using the HttpOnly cookie
      // This call will use withCredentials to send the refresh token cookie
      await authService.refreshToken();

      // Successfully refreshed, now fetch user profile
      const user = await authService.getCurrentUser();

      return user;
    } catch (error: any) {
      // Refresh token is invalid, expired, or doesn't exist
      // User needs to login again
      return rejectWithValue(error.message || 'Sessão expirada');
    }
  }
);

export const loginUser = createAsyncThunk(
  'auth/login',
  async (credentials: LoginRequest, { rejectWithValue }) => {
    try {
      const response = await authService.login(credentials);
      return { usuario: response.usuario, rememberMe: credentials.rememberMe || false };
    } catch (error: any) {
      return rejectWithValue(error.message || 'Erro ao fazer login');
    }
  }
);

export const logoutUser = createAsyncThunk(
  'auth/logout',
  async (_, { rejectWithValue }) => {
    try {
      await authService.logout();
    } catch (error: any) {
      return rejectWithValue(error.message || 'Erro ao fazer logout');
    }
  }
);

export const getCurrentUser = createAsyncThunk(
  'auth/getCurrentUser',
  async (_, { rejectWithValue }) => {
    try {
      const user = await authService.getCurrentUser();
      return user;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Erro ao buscar usuário');
    }
  }
);

/**
 * Auth slice
 */
const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setUser: (state, action: PayloadAction<Usuario | null>) => {
      state.user = action.payload;
      state.isAuthenticated = !!action.payload;
    },
    clearError: (state) => {
      state.error = null;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload;
    },
  },
  extraReducers: (builder) => {
    // Initialize auth
    builder
      .addCase(initializeAuth.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(initializeAuth.fulfilled, (state, action) => {
        state.isLoading = false;
        if (action.payload) {
          state.user = action.payload;
          state.isAuthenticated = true;
        } else {
          // No token or token expired
          state.user = null;
          state.isAuthenticated = false;
        }
        state.error = null;
      })
      .addCase(initializeAuth.rejected, (state) => {
        // Token is invalid, clear auth state
        state.isLoading = false;
        state.user = null;
        state.isAuthenticated = false;
        state.error = null; // Don't show error for initialization failure
      });

    // Login
    builder
      .addCase(loginUser.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(loginUser.fulfilled, (state, action) => {
        state.isLoading = false;
        state.user = action.payload.usuario;
        state.isAuthenticated = true;
        state.error = null;

        // Save to localStorage if remember me is active
        saveUserToStorage(action.payload.usuario, action.payload.rememberMe);
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });

    // Logout
    builder
      .addCase(logoutUser.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(logoutUser.fulfilled, (state) => {
        state.isLoading = false;
        state.user = null;
        state.isAuthenticated = false;
        state.error = null;

        // Clear localStorage
        clearUserFromStorage();
      })
      .addCase(logoutUser.rejected, (state) => {
        // Force logout even on error
        state.isLoading = false;
        state.user = null;
        state.isAuthenticated = false;

        // Clear localStorage even on error
        clearUserFromStorage();
      });

    // Get current user
    builder
      .addCase(getCurrentUser.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(getCurrentUser.fulfilled, (state, action) => {
        state.isLoading = false;
        state.user = action.payload;
        state.isAuthenticated = true;
        state.error = null;
      })
      .addCase(getCurrentUser.rejected, (state, action) => {
        state.isLoading = false;
        state.user = null;
        state.isAuthenticated = false;
        state.error = action.payload as string;
      });
  },
});

export const { setUser, clearError, setLoading } = authSlice.actions;
export default authSlice.reducer;
