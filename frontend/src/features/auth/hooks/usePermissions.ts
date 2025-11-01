import { useAuth } from './useAuth';
import { PerfilUsuario } from '../types';

/**
 * Hook for role-based authorization
 */
export const usePermissions = () => {
  const { user } = useAuth();

  const hasRole = (role: PerfilUsuario | PerfilUsuario[]) => {
    if (!user) return false;

    if (Array.isArray(role)) {
      return role.includes(user.perfil);
    }

    return user.perfil === role;
  };

  const isAdmin = () => hasRole(PerfilUsuario.ADMIN);
  const isGerente = () => hasRole(PerfilUsuario.GERENTE);
  const isAtendente = () => hasRole(PerfilUsuario.ATENDENTE);
  const isMecanico = () => hasRole(PerfilUsuario.MECANICO);

  const canManageUsers = () => isAdmin();
  const canManageFinancial = () => hasRole([PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]);
  const canManageOrders = () => hasRole([PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]);
  const canViewOrders = () => !!user; // All authenticated users
  const canUpdateOrderStatus = () => hasRole([PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.MECANICO]);
  const canManageInventory = () => hasRole([PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]);
  const canViewInventory = () => !!user; // All authenticated users

  return {
    user,
    hasRole,
    isAdmin,
    isGerente,
    isAtendente,
    isMecanico,
    canManageUsers,
    canManageFinancial,
    canManageOrders,
    canViewOrders,
    canUpdateOrderStatus,
    canManageInventory,
    canViewInventory,
  };
};
