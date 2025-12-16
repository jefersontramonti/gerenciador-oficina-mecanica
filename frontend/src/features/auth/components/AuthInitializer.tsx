import { useEffect, useState } from 'react';
import { useAppDispatch } from '@/shared/hooks/useAppDispatch';
import { initializeAuth } from '../store/authSlice';

interface AuthInitializerProps {
  children: React.ReactNode;
}

/**
 * Component that initializes authentication on app startup
 * Checks if there's a valid token and fetches user data if needed
 */
export const AuthInitializer = ({ children }: AuthInitializerProps) => {
  const dispatch = useAppDispatch();
  const [isInitialized, setIsInitialized] = useState(false);

  useEffect(() => {
    const initialize = async () => {
      try {
        await dispatch(initializeAuth()).unwrap();
      } catch (error) {
        // Silently fail - user will be redirected to login if needed
        console.debug('Auth initialization failed:', error);
      } finally {
        setIsInitialized(true);
      }
    };

    initialize();
  }, [dispatch]);

  // Show loading spinner while initializing
  if (!isInitialized) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="h-12 w-12 animate-spin rounded-full border-4 border-blue-600 border-t-transparent" />
      </div>
    );
  }

  return <>{children}</>;
};
