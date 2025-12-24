/**
 * Hooks React Query para operações de configurações do usuário
 */

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { authService } from '@/features/auth/services/authService';
import { useAppDispatch } from '@/shared/hooks';
import { setUser } from '@/features/auth/store/authSlice';
import type { Usuario } from '@/features/auth/types';

interface UpdateProfileData {
  nome: string;
}

interface ChangePasswordData {
  currentPassword: string;
  newPassword: string;
}

/**
 * Hook para atualizar o perfil do usuário
 */
export const useUpdateProfile = () => {
  const queryClient = useQueryClient();
  const dispatch = useAppDispatch();

  return useMutation({
    mutationFn: (data: UpdateProfileData) => authService.updateProfile(data),
    onSuccess: (updatedUser: Usuario) => {
      // Atualizar o usuário no Redux
      dispatch(setUser(updatedUser));

      // Invalidar cache do usuário
      queryClient.invalidateQueries({ queryKey: ['auth', 'me'] });

      toast.success('Perfil atualizado com sucesso!');
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Erro ao atualizar perfil';
      toast.error('Erro ao atualizar perfil', {
        description: message,
      });
    },
  });
};

/**
 * Hook para alterar a senha do usuário
 */
export const useChangePassword = () => {
  return useMutation({
    mutationFn: (data: ChangePasswordData) =>
      authService.changePassword(data.currentPassword, data.newPassword),
    onSuccess: () => {
      toast.success('Senha alterada com sucesso!', {
        description: 'Use sua nova senha no próximo login.',
      });
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Erro ao alterar senha';
      toast.error('Erro ao alterar senha', {
        description: message,
      });
    },
  });
};
