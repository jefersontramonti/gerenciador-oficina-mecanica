/**
 * Formulário para edição do perfil do usuário
 */

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { User, Mail, Shield, Calendar } from 'lucide-react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { useUpdateProfile } from '../hooks/useConfiguracoes';
import { formatDate } from '@/shared/utils/formatters';

const perfilSchema = z.object({
  nome: z
    .string()
    .min(3, 'Nome deve ter no mínimo 3 caracteres')
    .max(100, 'Nome deve ter no máximo 100 caracteres'),
});

type PerfilFormData = z.infer<typeof perfilSchema>;

export const PerfilForm = () => {
  const { user } = useAuth();
  const updateProfile = useUpdateProfile();

  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<PerfilFormData>({
    resolver: zodResolver(perfilSchema),
    defaultValues: {
      nome: user?.nome || '',
    },
  });

  const onSubmit = async (data: PerfilFormData) => {
    await updateProfile.mutateAsync(data);
  };

  const formatUserDate = (date: string | number[] | null | undefined) => {
    if (!date) return '-';
    if (Array.isArray(date)) {
      const [year, month, day, hour = 0, minute = 0] = date;
      return new Date(year, month - 1, day, hour, minute).toLocaleString('pt-BR');
    }
    return formatDate(date);
  };

  const getPerfilLabel = (perfil: string) => {
    const labels: Record<string, string> = {
      ADMIN: 'Administrador',
      GERENTE: 'Gerente',
      ATENDENTE: 'Atendente',
      MECANICO: 'Mecânico',
    };
    return labels[perfil] || perfil;
  };

  return (
    <div className="space-y-6">
      {/* Informações somente leitura */}
      <div className="rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 p-4">
        <h3 className="mb-4 text-sm font-medium text-gray-700 dark:text-gray-300">
          Informações da conta
        </h3>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex items-center gap-3">
            <Mail className="h-4 w-4 text-gray-500 dark:text-gray-400" />
            <div>
              <p className="text-xs text-gray-500 dark:text-gray-400">E-mail</p>
              <p className="font-medium text-gray-900 dark:text-white">{user?.email}</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <Shield className="h-4 w-4 text-gray-500 dark:text-gray-400" />
            <div>
              <p className="text-xs text-gray-500 dark:text-gray-400">Perfil</p>
              <p className="font-medium text-gray-900 dark:text-white">{getPerfilLabel(user?.perfil || '')}</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <Calendar className="h-4 w-4 text-gray-500 dark:text-gray-400" />
            <div>
              <p className="text-xs text-gray-500 dark:text-gray-400">Último acesso</p>
              <p className="font-medium text-gray-900 dark:text-white">{formatUserDate(user?.ultimoAcesso)}</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <Calendar className="h-4 w-4 text-gray-500 dark:text-gray-400" />
            <div>
              <p className="text-xs text-gray-500 dark:text-gray-400">Conta criada em</p>
              <p className="font-medium text-gray-900 dark:text-white">{formatUserDate(user?.createdAt)}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Formulário de edição */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label htmlFor="nome" className="mb-1 flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <User className="h-4 w-4" />
            Nome completo
          </label>
          <input
            id="nome"
            type="text"
            placeholder="Seu nome completo"
            className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            {...register('nome')}
          />
          {errors.nome && (
            <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.nome.message}</p>
          )}
        </div>

        <div className="flex justify-end pt-4">
          <button
            type="submit"
            disabled={updateProfile.isPending || !isDirty}
            className="rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {updateProfile.isPending ? 'Salvando...' : 'Salvar alterações'}
          </button>
        </div>
      </form>
    </div>
  );
};
