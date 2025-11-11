/**
 * Página de reset de senha
 * Recebe token via query string: /reset-password?token=xxx
 */

import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { authService } from '../services/authService';
import { AlertCircle, CheckCircle, ArrowLeft, Eye, EyeOff, Lock } from 'lucide-react';

const resetPasswordSchema = z
  .object({
    newPassword: z
      .string()
      .min(6, 'A senha deve ter no mínimo 6 caracteres')
      .regex(/[A-Z]/, 'A senha deve conter ao menos uma letra maiúscula')
      .regex(/[a-z]/, 'A senha deve conter ao menos uma letra minúscula')
      .regex(/[0-9]/, 'A senha deve conter ao menos um número'),
    confirmPassword: z.string().min(1, 'Confirme sua senha'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'As senhas não coincidem',
    path: ['confirmPassword'],
  });

type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>;

export const ResetPasswordPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [apiError, setApiError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetPasswordFormData>({
    resolver: zodResolver(resetPasswordSchema),
  });

  // Validar se token existe ao carregar a página
  useEffect(() => {
    if (!token) {
      setApiError('Token de recuperação inválido ou ausente. Solicite um novo link.');
    }
  }, [token]);

  const onSubmit = async (data: ResetPasswordFormData) => {
    if (!token) {
      setApiError('Token de recuperação inválido');
      return;
    }

    try {
      setApiError(null);
      setIsLoading(true);

      await authService.resetPassword({
        token,
        newPassword: data.newPassword,
      });

      setIsSuccess(true);

      // Redirecionar para login após 3 segundos
      setTimeout(() => {
        navigate('/login', { replace: true });
      }, 3000);
    } catch (err: any) {
      const message = err.response?.data?.message || err.message;

      if (message?.includes('expirado') || message?.includes('expired')) {
        setApiError('Token expirado. Por favor, solicite um novo link de recuperação.');
      } else if (message?.includes('inválido') || message?.includes('invalid')) {
        setApiError('Token inválido. Verifique o link recebido por email.');
      } else {
        setApiError(message || 'Erro ao redefinir senha. Tente novamente.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-b from-gray-900 to-black px-4 py-8">
      {/* Card */}
      <div className="w-full max-w-md rounded-xl border border-gray-700 bg-gray-800 p-8 shadow-2xl">
        {/* Cabeçalho com Logo e Título */}
        <div className="mb-8 text-center">
          {/* Ícone */}
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center">
            <Lock className="h-16 w-16 text-blue-500" />
          </div>

          <h1 className="text-3xl font-bold text-white">Redefinir Senha</h1>
          <p className="mt-2 text-gray-400">Digite sua nova senha</p>
        </div>

        {/* Mensagem de Sucesso */}
        {isSuccess && (
          <div className="mb-6 flex items-start gap-2 rounded-lg border border-green-800 bg-green-900/20 p-4 text-sm text-green-400">
            <CheckCircle className="h-5 w-5 flex-shrink-0" />
            <div>
              <p className="font-medium">Senha redefinida com sucesso!</p>
              <p className="mt-1 text-green-300">
                Você será redirecionado para o login em alguns segundos...
              </p>
            </div>
          </div>
        )}

        {/* Mensagem de Erro */}
        {apiError && (
          <div className="mb-6 flex items-start gap-2 rounded-lg border border-red-800 bg-red-900/20 p-3 text-sm text-red-400">
            <AlertCircle className="h-5 w-5 flex-shrink-0" />
            <span>{apiError}</span>
          </div>
        )}

        {/* Formulário */}
        {!isSuccess && token && (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            {/* Nova Senha */}
            <div>
              <label
                htmlFor="newPassword"
                className="mb-2 block text-sm font-medium text-gray-300"
              >
                Nova Senha
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="newPassword"
                  {...register('newPassword')}
                  className="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2.5 pr-10 text-white placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="••••••••"
                  disabled={isLoading}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-300"
                >
                  {showPassword ? (
                    <EyeOff className="h-5 w-5" />
                  ) : (
                    <Eye className="h-5 w-5" />
                  )}
                </button>
              </div>
              {errors.newPassword && (
                <p className="mt-1 text-sm text-red-400">{errors.newPassword.message}</p>
              )}
            </div>

            {/* Confirmar Senha */}
            <div>
              <label
                htmlFor="confirmPassword"
                className="mb-2 block text-sm font-medium text-gray-300"
              >
                Confirmar Nova Senha
              </label>
              <div className="relative">
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  id="confirmPassword"
                  {...register('confirmPassword')}
                  className="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-2.5 pr-10 text-white placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="••••••••"
                  disabled={isLoading}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-300"
                >
                  {showConfirmPassword ? (
                    <EyeOff className="h-5 w-5" />
                  ) : (
                    <Eye className="h-5 w-5" />
                  )}
                </button>
              </div>
              {errors.confirmPassword && (
                <p className="mt-1 text-sm text-red-400">{errors.confirmPassword.message}</p>
              )}
            </div>

            {/* Requisitos de Senha */}
            <div className="rounded-lg border border-gray-700 bg-gray-700/50 p-3 text-xs text-gray-400">
              <p className="mb-2 font-medium text-gray-300">A senha deve conter:</p>
              <ul className="ml-4 list-disc space-y-1">
                <li>Mínimo de 6 caracteres</li>
                <li>Pelo menos uma letra maiúscula</li>
                <li>Pelo menos uma letra minúscula</li>
                <li>Pelo menos um número</li>
              </ul>
            </div>

            {/* Botão Redefinir */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full rounded-lg bg-blue-600 px-4 py-2.5 font-medium text-white transition hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500/50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isLoading ? 'Redefinindo...' : 'Redefinir Senha'}
            </button>
          </form>
        )}

        {/* Link para solicitar novo token se expirou */}
        {apiError && apiError.includes('expirado') && (
          <div className="mt-4 text-center">
            <Link
              to="/forgot-password"
              className="text-sm text-blue-400 hover:text-blue-300 hover:underline"
            >
              Solicitar novo link de recuperação
            </Link>
          </div>
        )}

        {/* Link de Voltar */}
        <div className="mt-6 text-center">
          <Link
            to="/login"
            className="inline-flex items-center gap-1 text-sm text-gray-400 transition hover:text-blue-400"
          >
            <ArrowLeft className="h-4 w-4" />
            Voltar para Login
          </Link>
        </div>
      </div>
    </div>
  );
};
