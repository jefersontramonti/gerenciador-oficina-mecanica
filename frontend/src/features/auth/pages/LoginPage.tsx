import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../hooks/useAuth';
import { AlertCircle } from 'lucide-react';

const loginSchema = z.object({
  email: z.string().min(1, 'Usuário é obrigatório'),
  senha: z.string().min(1, 'Senha é obrigatória'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export const LoginPage = () => {
  const navigate = useNavigate();
  const { login, isLoading, error } = useAuth();
  const [apiError, setApiError] = useState<string | null>(null);
  const [rememberMe, setRememberMe] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      setApiError(null);
      const result = await login({ ...data, rememberMe });
      // Redireciona baseado no perfil do usuário
      if (result.usuario.perfil === 'SUPER_ADMIN') {
        navigate('/admin', { replace: true });
      } else {
        navigate('/', { replace: true });
      }
    } catch (err: any) {
      setApiError(err.message || 'Erro ao fazer login');
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-b from-gray-900 to-black px-4 py-8">
      {/* Card de Login */}
      <div className="w-full max-w-md rounded-xl border border-gray-700 bg-gray-800 p-8 shadow-2xl">

        {/* Cabeçalho com Logo e Título */}
        <div className="mb-8 text-center">
          {/* Ícone SVG - Engrenagem e Chave Inglesa */}
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center">
            <svg
              className="h-16 w-16 text-blue-500"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth="1.5"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M11.42 15.17L17.25 21A2.652 2.652 0 0021 17.25l-5.83-5.83M11.42 15.17l.043.043m-1.42-1.42l-1.498 1.498m1.498-1.498l.043.043m-4.28 4.28l.043.043m-1.42-1.42l-1.498 1.498m1.498-1.498l.043.043m-4.28 4.28l.043.043m-1.42-1.42l-1.498 1.498m1.498-1.498l.043.043m-4.28 4.28l.043.043m-1.42-1.42l-1.498 1.498m1.498-1.498l.043.043M3.75 3.75c.621 0 1.125.504 1.125 1.125v1.5c0 .621-.504 1.125-1.125 1.125H2.625c-.621 0-1.125-.504-1.125-1.125v-1.5c0-.621.504-1.125 1.125-1.125H3.75zM14.25 3.75c.621 0 1.125.504 1.125 1.125v1.5c0 .621-.504 1.125-1.125 1.125h-1.125c-.621 0-1.125-.504-1.125-1.125v-1.5c0-.621.504-1.125 1.125-1.125h1.125zM21 3.75c.621 0 1.125.504 1.125 1.125v1.5c0 .621-.504 1.125-1.125 1.125h-1.125c-.621 0-1.125-.504-1.125-1.125v-1.5c0-.621.504-1.125 1.125-1.125h1.125zM3.75 14.25c.621 0 1.125.504 1.125 1.125v1.5c0 .621-.504 1.125-1.125 1.125H2.625c-.621 0-1.125-.504-1.125-1.125v-1.5c0-.621.504-1.125 1.125-1.125H3.75zM14.25 14.25c.621 0 1.125.504 1.125 1.125v1.5c0 .621-.504 1.125-1.125 1.125h-1.125c-.621 0-1.125-.504-1.125-1.125v-1.5c0-.621.504-1.125 1.125-1.125h1.125z"
              />
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M15.964 2.25c-.53 0-1.02.214-1.384.578l-4.26 4.26a.75.75 0 000 1.06l4.26 4.26c.364.364.854.578 1.384.578h1.104c.53 0 1.02-.214 1.384-.578l4.26-4.26a.75.75 0 000-1.06l-4.26-4.26A1.95 1.95 0 0017.068 2.25h-1.104z"
              />
            </svg>
          </div>

          <h1 className="text-3xl font-bold text-white">PitStop</h1>
          <p className="mt-2 text-gray-400">Sistema de Gerenciamento de Oficina</p>
        </div>

        {/* Mensagem de Erro */}
        {(apiError || error) && (
          <div className="mb-6 flex items-start gap-2 rounded-lg border border-red-800 bg-red-900/20 p-3 text-sm text-red-400">
            <AlertCircle className="h-5 w-5 flex-shrink-0" />
            <span>{apiError || error}</span>
          </div>
        )}

        {/* Formulário de Login */}
        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="space-y-6">

            {/* Campo Usuário */}
            <div>
              <label htmlFor="email" className="mb-2 block text-sm font-medium text-gray-300">
                Usuário
              </label>
              <input
                {...register('email')}
                type="text"
                id="email"
                placeholder="seu.usuario"
                className="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-3 text-white placeholder-gray-500 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {errors.email && (
                <p className="mt-1 text-sm text-red-400">{errors.email.message}</p>
              )}
            </div>

            {/* Campo Senha */}
            <div>
              <label htmlFor="senha" className="mb-2 block text-sm font-medium text-gray-300">
                Senha
              </label>
              <input
                {...register('senha')}
                type="password"
                id="senha"
                placeholder="********"
                className="w-full rounded-lg border border-gray-600 bg-gray-700 px-4 py-3 text-white placeholder-gray-500 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {errors.senha && (
                <p className="mt-1 text-sm text-red-400">{errors.senha.message}</p>
              )}
            </div>

            {/* Opções Adicionais */}
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <input
                  id="remember-me"
                  name="remember-me"
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="h-4 w-4 rounded border-gray-600 bg-gray-700 text-blue-600 focus:ring-blue-500"
                />
                <label htmlFor="remember-me" className="ml-2 block text-sm text-gray-300">
                  Mantenha-me conectado
                </label>
              </div>

              <div className="text-sm">
                <Link
                  to="/forgot-password"
                  className="font-medium text-blue-400 hover:text-blue-300 hover:underline"
                >
                  Esqueceu sua senha?
                </Link>
              </div>
            </div>

            {/* Botão Entrar */}
            <div>
              <button
                type="submit"
                disabled={isLoading}
                className="w-full rounded-lg bg-blue-600 px-4 py-3 font-bold text-white shadow-lg transition-colors duration-200 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isLoading ? 'Entrando...' : 'Entrar'}
              </button>
            </div>
          </div>
        </form>

        {/* Link para Cadastre-se */}
        <div className="mt-6 text-center">
          <p className="text-sm text-gray-400">
            Não tem uma conta?{' '}
            <Link
              to="/register"
              className="font-medium text-blue-400 hover:text-blue-300 hover:underline"
            >
              Cadastre-se
            </Link>
          </p>
        </div>

        {/* Rodapé do Card */}
        <div className="mt-8 text-center">
          <p className="text-sm text-gray-500">
            &copy; 2025 PitStop Cloud. Todos os direitos reservados.
          </p>
        </div>
      </div>
    </div>
  );
};
