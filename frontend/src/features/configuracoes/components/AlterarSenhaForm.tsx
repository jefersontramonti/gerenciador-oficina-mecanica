/**
 * Formulário para alteração de senha do usuário
 */

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Lock, Eye, EyeOff, ShieldCheck, AlertTriangle } from 'lucide-react';
import { useChangePassword } from '../hooks/useConfiguracoes';

const alterarSenhaSchema = z
  .object({
    currentPassword: z.string().min(1, 'Senha atual é obrigatória'),
    newPassword: z
      .string()
      .min(8, 'Nova senha deve ter no mínimo 8 caracteres')
      .regex(/[A-Z]/, 'Deve conter pelo menos uma letra maiúscula')
      .regex(/[a-z]/, 'Deve conter pelo menos uma letra minúscula')
      .regex(/[0-9]/, 'Deve conter pelo menos um número'),
    confirmPassword: z.string().min(1, 'Confirmação de senha é obrigatória'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'As senhas não coincidem',
    path: ['confirmPassword'],
  })
  .refine((data) => data.currentPassword !== data.newPassword, {
    message: 'A nova senha deve ser diferente da atual',
    path: ['newPassword'],
  });

type AlterarSenhaFormData = z.infer<typeof alterarSenhaSchema>;

export const AlterarSenhaForm = () => {
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const changePassword = useChangePassword();

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<AlterarSenhaFormData>({
    resolver: zodResolver(alterarSenhaSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const newPassword = watch('newPassword');

  const onSubmit = async (data: AlterarSenhaFormData) => {
    await changePassword.mutateAsync({
      currentPassword: data.currentPassword,
      newPassword: data.newPassword,
    });
    reset();
  };

  const passwordRequirements = [
    { label: 'Mínimo 8 caracteres', met: newPassword?.length >= 8 },
    { label: 'Uma letra maiúscula', met: /[A-Z]/.test(newPassword || '') },
    { label: 'Uma letra minúscula', met: /[a-z]/.test(newPassword || '') },
    { label: 'Um número', met: /[0-9]/.test(newPassword || '') },
  ];

  return (
    <div className="space-y-6">
      {/* Aviso de segurança */}
      <div className="rounded-lg border border-yellow-200 dark:border-yellow-800 bg-yellow-50 dark:bg-yellow-900/20 p-4">
        <div className="flex gap-3">
          <AlertTriangle className="h-5 w-5 flex-shrink-0 text-yellow-600 dark:text-yellow-400" />
          <div>
            <h4 className="font-medium text-yellow-800 dark:text-yellow-200">
              Dica de segurança
            </h4>
            <p className="mt-1 text-sm text-yellow-700 dark:text-yellow-300">
              Use uma senha forte e única. Nunca compartilhe sua senha com outras pessoas.
            </p>
          </div>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {/* Senha atual */}
        <div>
          <label htmlFor="currentPassword" className="mb-1 flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <Lock className="h-4 w-4" />
            Senha atual
          </label>
          <div className="relative">
            <input
              id="currentPassword"
              type={showCurrentPassword ? 'text' : 'password'}
              placeholder="Digite sua senha atual"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 pr-10 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              {...register('currentPassword')}
            />
            <button
              type="button"
              onClick={() => setShowCurrentPassword(!showCurrentPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
            >
              {showCurrentPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
          {errors.currentPassword && (
            <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.currentPassword.message}</p>
          )}
        </div>

        {/* Nova senha */}
        <div>
          <label htmlFor="newPassword" className="mb-1 flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <ShieldCheck className="h-4 w-4" />
            Nova senha
          </label>
          <div className="relative">
            <input
              id="newPassword"
              type={showNewPassword ? 'text' : 'password'}
              placeholder="Digite sua nova senha"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 pr-10 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              {...register('newPassword')}
            />
            <button
              type="button"
              onClick={() => setShowNewPassword(!showNewPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
            >
              {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
          {errors.newPassword && (
            <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.newPassword.message}</p>
          )}

          {/* Requisitos da senha */}
          {newPassword && (
            <div className="mt-2 rounded-md bg-gray-50 dark:bg-gray-900 p-3 border border-gray-200 dark:border-gray-700">
              <p className="mb-2 text-xs font-medium text-gray-700 dark:text-gray-300">
                Requisitos da senha:
              </p>
              <ul className="space-y-1">
                {passwordRequirements.map((req) => (
                  <li
                    key={req.label}
                    className={`flex items-center gap-2 text-xs ${
                      req.met ? 'text-green-600 dark:text-green-400' : 'text-gray-500 dark:text-gray-400'
                    }`}
                  >
                    <span
                      className={`h-1.5 w-1.5 rounded-full ${
                        req.met ? 'bg-green-600 dark:bg-green-400' : 'bg-gray-400 dark:bg-gray-500'
                      }`}
                    />
                    {req.label}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>

        {/* Confirmar nova senha */}
        <div>
          <label htmlFor="confirmPassword" className="mb-1 flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <Lock className="h-4 w-4" />
            Confirmar nova senha
          </label>
          <div className="relative">
            <input
              id="confirmPassword"
              type={showConfirmPassword ? 'text' : 'password'}
              placeholder="Confirme sua nova senha"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 pr-10 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              {...register('confirmPassword')}
            />
            <button
              type="button"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
            >
              {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
          {errors.confirmPassword && (
            <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.confirmPassword.message}</p>
          )}
        </div>

        <div className="flex justify-end pt-4">
          <button
            type="submit"
            disabled={changePassword.isPending}
            className="rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {changePassword.isPending ? 'Alterando...' : 'Alterar senha'}
          </button>
        </div>
      </form>
    </div>
  );
};
