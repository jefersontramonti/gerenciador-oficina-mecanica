/**
 * Página de formulário para criar/editar usuário
 * Usado tanto para criação quanto edição (modo definido pela presença de ID na URL)
 */

import { useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ArrowLeft, Save } from 'lucide-react';
import { showError } from '@/shared/utils/notifications';
import {
  useUsuario,
  useCreateUsuario,
  useUpdateUsuario,
} from '../hooks/useUsuarios';

/**
 * Schema de validação Zod
 */
const createUsuarioSchema = z.object({
  nome: z.string().min(3, 'Nome deve ter no mínimo 3 caracteres'),
  email: z.string().email('Email inválido'),
  senha: z.string().min(6, 'Senha deve ter no mínimo 6 caracteres'),
  perfil: z.enum(['ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO'], {
    message: 'Selecione um perfil',
  }),
});

const updateUsuarioSchema = z.object({
  nome: z.string().min(3, 'Nome deve ter no mínimo 3 caracteres'),
  email: z.string().email('Email inválido'),
  senha: z.string().min(6, 'Senha deve ter no mínimo 6 caracteres').optional().or(z.literal('')),
  perfil: z.enum(['ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO'], {
    message: 'Selecione um perfil',
  }),
});

type CreateUsuarioFormData = z.infer<typeof createUsuarioSchema>;
type UpdateUsuarioFormData = z.infer<typeof updateUsuarioSchema>;

export const UsuarioFormPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditMode = !!id;

  // Queries e mutations
  const { data: usuario, isLoading: isLoadingUsuario } = useUsuario(id);
  const createMutation = useCreateUsuario();
  const updateMutation = useUpdateUsuario();

  // Form setup com validação condicional
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateUsuarioFormData | UpdateUsuarioFormData>({
    resolver: zodResolver(isEditMode ? updateUsuarioSchema : createUsuarioSchema),
  });

  // Preencher form em modo edição
  useEffect(() => {
    if (isEditMode && usuario) {
      reset({
        nome: usuario.nome,
        email: usuario.email,
        perfil: usuario.perfil,
        senha: '', // Não mostra senha existente
      });
    }
  }, [usuario, isEditMode, reset]);

  /**
   * Handler de submit
   */
  const onSubmit = async (data: CreateUsuarioFormData | UpdateUsuarioFormData) => {
    try {
      if (isEditMode && id) {
        // Atualização - remover senha se estiver vazia
        const updateData = { ...data };
        if (!updateData.senha || updateData.senha === '') {
          delete updateData.senha;
        }
        await updateMutation.mutateAsync({ id, data: updateData });
      } else {
        // Criação
        await createMutation.mutateAsync(data as CreateUsuarioFormData);
      }
      navigate('/usuarios');
    } catch (error: any) {
      if (error.response?.status === 409) {
        showError('Email já está em uso por outro usuário.');
      } else if (error.response?.status === 404) {
        showError('Usuário não encontrado.');
      } else {
        const errorMessage =
          error.response?.data?.message || error.message || 'Erro ao salvar usuário';
        showError(`Erro: ${errorMessage}`);
      }
    }
  };

  // Loading state
  if (isEditMode && isLoadingUsuario) {
    return (
      <div className="flex h-64 items-center justify-center">
        <p className="text-gray-600 dark:text-gray-400">Carregando usuário...</p>
      </div>
    );
  }

  // Not found (somente em modo edição)
  if (isEditMode && !isLoadingUsuario && !usuario) {
    return (
      <div className="p-4 sm:p-6">
        <div className="rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400 dark:text-red-300">
          Usuário não encontrado.
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-4 sm:mb-6">
        <div className="mb-4 flex items-center gap-3 sm:gap-4">
          <Link
            to="/usuarios"
            className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 p-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
          >
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100">
              {isEditMode ? 'Editar Usuário' : 'Novo Usuário'}
            </h1>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              {isEditMode
                ? 'Atualize as informações do usuário'
                : 'Preencha os dados para criar um novo usuário'}
            </p>
          </div>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 sm:space-y-6">
        {/* Card: Informações Básicas */}
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Informações Básicas</h2>

          <div className="grid gap-4 grid-cols-1 sm:grid-cols-2">
            {/* Nome */}
            <div>
              <label htmlFor="nome" className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Nome Completo <span className="text-red-600 dark:text-red-400">*</span>
              </label>
              <input
                type="text"
                id="nome"
                {...register('nome')}
                className={`w-full rounded-lg border px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 ${
                  errors.nome
                    ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                    : 'border-gray-300 dark:border-gray-600 focus:border-blue-500 focus:ring-blue-500/20'
                }`}
                placeholder="Ex: João da Silva"
              />
              {errors.nome && (
                <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.nome.message}</p>
              )}
            </div>

            {/* Email */}
            <div>
              <label htmlFor="email" className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Email <span className="text-red-600 dark:text-red-400">*</span>
              </label>
              <input
                type="email"
                id="email"
                {...register('email')}
                className={`w-full rounded-lg border px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 ${
                  errors.email
                    ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                    : 'border-gray-300 dark:border-gray-600 focus:border-blue-500 focus:ring-blue-500/20'
                }`}
                placeholder="usuario@exemplo.com"
              />
              {errors.email && (
                <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.email.message}</p>
              )}
            </div>

            {/* Perfil */}
            <div>
              <label htmlFor="perfil" className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Perfil de Acesso <span className="text-red-600 dark:text-red-400">*</span>
              </label>
              <select
                id="perfil"
                {...register('perfil')}
                className={`w-full rounded-lg border px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 ${
                  errors.perfil
                    ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                    : 'border-gray-300 dark:border-gray-600 focus:border-blue-500 focus:ring-blue-500/20'
                }`}
              >
                <option value="">Selecione o perfil</option>
                <option value="ADMIN">Administrador</option>
                <option value="GERENTE">Gerente</option>
                <option value="ATENDENTE">Atendente</option>
                <option value="MECANICO">Mecânico</option>
              </select>
              {errors.perfil && (
                <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.perfil.message}</p>
              )}
            </div>

            {/* Senha */}
            <div>
              <label htmlFor="senha" className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Senha {isEditMode ? '(deixe em branco para manter)' : <span className="text-red-600 dark:text-red-400">*</span>}
              </label>
              <input
                type="password"
                id="senha"
                {...register('senha')}
                className={`w-full rounded-lg border px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 ${
                  errors.senha
                    ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                    : 'border-gray-300 dark:border-gray-600 focus:border-blue-500 focus:ring-blue-500/20'
                }`}
                placeholder={isEditMode ? '******' : 'Mínimo 6 caracteres'}
              />
              {errors.senha && (
                <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.senha.message}</p>
              )}
            </div>
          </div>
        </div>

        {/* Card: Informações sobre Perfis */}
        <div className="rounded-lg bg-blue-50 dark:bg-blue-900/30 p-4 sm:p-6">
          <h3 className="mb-3 text-sm font-semibold text-blue-900 dark:text-blue-300">
            Informações sobre Perfis de Acesso
          </h3>
          <div className="space-y-2 text-xs sm:text-sm text-blue-800 dark:text-blue-200">
            <div>
              <strong>Administrador:</strong> Acesso completo ao sistema, incluindo gestão de usuários.
            </div>
            <div>
              <strong>Gerente:</strong> Acesso a todos os módulos exceto gestão de usuários. Pode aprovar descontos e ver relatórios financeiros.
            </div>
            <div>
              <strong>Atendente:</strong> CRUD de clientes, veículos e ordens de serviço. Pode visualizar estoque e registrar pagamentos.
            </div>
            <div>
              <strong>Mecânico:</strong> Visualiza ordens atribuídas, atualiza status e observações. Visualização de estoque somente leitura.
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-end">
          <Link
            to="/usuarios"
            className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-6 py-2 text-center text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
          >
            Cancelar
          </Link>
          <button
            type="submit"
            disabled={isSubmitting || createMutation.isPending || updateMutation.isPending}
            className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 dark:bg-blue-700 px-6 py-2 text-white hover:bg-blue-700 dark:hover:bg-blue-600 disabled:opacity-50"
          >
            <Save className="h-5 w-5" />
            {isSubmitting || createMutation.isPending || updateMutation.isPending
              ? 'Salvando...'
              : 'Salvar'}
          </button>
        </div>
      </form>
    </div>
  );
};
