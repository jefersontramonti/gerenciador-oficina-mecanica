import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { PerfilUsuario } from '@/features/auth/types';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRoles?: PerfilUsuario[];
}

/**
 * Protected route component that requires authentication
 * and optionally specific roles
 */
export const ProtectedRoute = ({ children, requiredRoles }: ProtectedRouteProps) => {
  const { isAuthenticated, user, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="h-12 w-12 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (!isAuthenticated) {
    // Redirect to login while saving the attempted location
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Check role-based access if roles are specified
  if (requiredRoles && requiredRoles.length > 0) {
    const hasRequiredRole = user && requiredRoles.includes(user.perfil);

    if (!hasRequiredRole) {
      // Redirect to unauthorized page
      return <Navigate to="/unauthorized" replace />;
    }
  }

  return <>{children}</>;
};
