import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import { authService } from '../services/authService';
import type { AuthState, LoginRequest, Usuario } from '../types';

// LocalStorage keys
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
  isAuthenticated: !!storedUser,
  isLoading: false,
  error: null,
};

/**
 * Async thunks for authentication
 */

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
