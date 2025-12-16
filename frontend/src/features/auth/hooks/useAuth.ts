import { useAppSelector } from '@/shared/hooks/useAppSelector';
import { useAppDispatch } from '@/shared/hooks/useAppDispatch';
import { loginUser, logoutUser, getCurrentUser, initializeAuth, clearError } from '../store/authSlice';
import type { LoginRequest } from '../types';

/**
 * Custom hook for authentication
 */
export const useAuth = () => {
  const dispatch = useAppDispatch();
  const { user, isAuthenticated, isLoading, error } = useAppSelector((state) => state.auth);

  const login = async (credentials: LoginRequest) => {
    return dispatch(loginUser(credentials)).unwrap();
  };

  const logout = async () => {
    return dispatch(logoutUser()).unwrap();
  };

  const fetchCurrentUser = async () => {
    return dispatch(getCurrentUser()).unwrap();
  };

  const initialize = async () => {
    return dispatch(initializeAuth()).unwrap();
  };

  const clearAuthError = () => {
    dispatch(clearError());
  };

  return {
    user,
    isAuthenticated,
    isLoading,
    error,
    login,
    logout,
    fetchCurrentUser,
    initialize,
    clearAuthError,
  };
};
