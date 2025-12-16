import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import { authService } from '../services/authService';
import { getAccessToken } from '@/shared/services/api';
import type { AuthState, LoginRequest, Usuario } from '../types';

// LocalStorage keys
const STORAGE_KEY_USER = 'pitstop_user';
const STORAGE_KEY_REMEMBER = 'pitstop_remember_me';
const TOKEN_STORAGE_KEY = 'pitstop_access_token';

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

// Check if there is a valid token in localStorage
const hasValidToken = (): boolean => {
  try {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    return !!token;
  } catch (error) {
    console.error('Error checking token:', error);
    return false;
  }
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

// If there's no stored user but there's a token, we need to fetch the user
// This will be handled by the initializeAuth thunk
const initialState: AuthState = {
  user: storedUser,
  isAuthenticated: !!storedUser,
  isLoading: hasValidToken() && !storedUser, // Set loading if token exists but no user
  error: null,
};

/**
 * Async thunks for authentication
 */

/**
 * Initialize authentication on app startup
 * If there's a token but no user data, fetch the user
 */
export const initializeAuth = createAsyncThunk(
  'auth/initialize',
  async (_, { rejectWithValue }) => {
    try {
      // Check if there's a token
      const token = getAccessToken();
      if (!token) {
        return null; // No token, no user
      }

      // Check if we already have user data (remember me was active)
      const storedUser = loadUserFromStorage();
      if (storedUser) {
        return storedUser; // User already loaded from storage
      }

      // We have a token but no user data, fetch from server
      const user = await authService.getCurrentUser();
      return user;
    } catch (error: any) {
      // Token is invalid or expired, clear it
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
