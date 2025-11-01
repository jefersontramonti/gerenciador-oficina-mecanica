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
import {
  useUsuario,
  useCreateUsuario,
  useUpdateUsuario,
} from '../hooks/useUsuarios';
import type { PerfilUsuario } from '../types';

/**
 * Schema de validação Zod
 */
const createUsuarioSchema = z.object({
  nome: z.string().min(3, 'Nome deve ter no mínimo 3 caracteres'),
  email: z.string().email('Email inválido'),
  senha: z.string().min(6, 'Senha deve ter no mínimo 6 caracteres'),
  perfil: z.enum(['ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO'], {
    required_error: 'Selecione um perfil',
  }),
});

const updateUsuarioSchema = z.object({
  nome: z.string().min(3, 'Nome deve ter no mínimo 3 caracteres'),
  email: z.string().email('Email inválido'),
  senha: z.string().min(6, 'Senha deve ter no mínimo 6 caracteres').optional().or(z.literal('')),
  perfil: z.enum(['ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO'], {
    required_error: 'Selecione um perfil',
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
        alert('Email já está em uso por outro usuário.');
      } else if (error.response?.status === 404) {
        alert('Usuário não encontrado.');
      } else {
        const errorMessage =
          error.response?.data?.message || error.message || 'Erro ao salvar usuário';
        alert(`Erro: ${errorMessage}`);
      }
    }
  };

  // Loading state
  if (isEditMode && isLoadingUsuario) {
    return (
      <div className="flex h-64 items-center justify-center">
        <p className="text-gray-600">Carregando usuário...</p>
      </div>
    );
  }

  // Not found (somente em modo edição)
  if (isEditMode && !isLoadingUsuario && !usuario) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Usuário não encontrado.
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6">
        <div className="mb-4 flex items-center gap-4">
          <Link
            to="/usuarios"
            className="rounded-lg border border-gray-300 p-2 text-gray-700 hover:bg-gray-50"
          >
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              {isEditMode ? 'Editar Usuário' : 'Novo Usuário'}
            </h1>
            <p className="mt-1 text-sm text-gray-600">
              {isEditMode
                ? 'Atualize as informações do usuário'
                : 'Preencha os dados para criar um novo usuário no sistema'}
            </p>
          </div>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Card: Informações Básicas */}
        <div className="rounded-lg bg-white p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900">Informações Básicas</h2>

          <div className="grid gap-4 md:grid-cols-2">
            {/* Nome */}
            <div>
              <label htmlFor="nome" className="mb-1 block text-sm font-medium text-gray-700">
                Nome Completo <span className="text-red-600">*</span>
              </label>
              <input
                type="text"
                id="nome"
                {...register('nome')}
                className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                  errors.nome
                    ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                    : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500/20'
                }`}
                placeholder="Ex: João da Silva"
              />
              {errors.nome && (
                <p className="mt-1 text-sm text-red-600">{errors.nome.message}</p>
              )}
            </div>

            {/* Email */}
            <div>
              <label htmlFor="email" className="mb-1 block text-sm font-medium text-gray-700">
                Email <span className="text-red-600">*</span>
              </label>
              <input
                type="email"
                id="email"
                {...register('email')}
                className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                  errors.email
                    ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                    : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500/20'
                }`}
                placeholder="usuario@exemplo.com"
              />
              {errors.email && (
                <p className="mt-1 text-sm text-red-600">{errors.email.message}</p>
              )}
            </div>

            {/* Perfil */}
            <div>
              <label htmlFor="perfil" className="mb-1 block text-sm font-medium text-gray-700">
                Perfil de Acesso <span className="text-red-600">*</span>
              </label>
              <select
                id="perfil"
                {...register('perfil')}
                className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                  errors.perfil
                    ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                    : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500/20'
                }`}
              >
                <option value="">Selecione o perfil</option>
                <option value="ADMIN">Administrador</option>
                <option value="GERENTE">Gerente</option>
                <option value="ATENDENTE">Atendente</option>
                <option value="MECANICO">Mecânico</option>
              </select>
              {errors.perfil && (
                <p className="mt-1 text-sm text-red-600">{errors.perfil.message}</p>
              )}
            </div>

            {/* Senha */}
            <div>
              <label htmlFor="senha" className="mb-1 block text-sm font-medium text-gray-700">
                Senha {isEditMode ? '(deixe em branco para manter)' : <span className="text-red-600">*</span>}
              </label>
              <input
                type="password"
                id="senha"
                {...register('senha')}
                className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 ${
                  errors.senha
                    ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                    : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500/20'
                }`}
                placeholder={isEditMode ? '******' : 'Mínimo 6 caracteres'}
              />
              {errors.senha && (
                <p className="mt-1 text-sm text-red-600">{errors.senha.message}</p>
              )}
            </div>
          </div>
        </div>

        {/* Card: Informações sobre Perfis */}
        <div className="rounded-lg bg-blue-50 p-6">
          <h3 className="mb-3 text-sm font-semibold text-blue-900">
            Informações sobre Perfis de Acesso
          </h3>
          <div className="space-y-2 text-sm text-blue-800">
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
        <div className="flex items-center justify-end gap-3">
          <Link
            to="/usuarios"
            className="rounded-lg border border-gray-300 px-6 py-2 text-gray-700 hover:bg-gray-50"
          >
            Cancelar
          </Link>
          <button
            type="submit"
            disabled={isSubmitting || createMutation.isPending || updateMutation.isPending}
            className="flex items-center gap-2 rounded-lg bg-blue-600 px-6 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
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
