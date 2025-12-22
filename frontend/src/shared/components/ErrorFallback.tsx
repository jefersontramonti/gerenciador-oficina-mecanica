import { AlertTriangle, RefreshCw, Home } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface ErrorFallbackProps {
  /**
   * The error that was caught
   */
  error: Error;

  /**
   * Function to reset the error and try again
   */
  resetError: () => void;

  /**
   * Optional: show detailed error info
   * @default false in production, true in development
   */
  showDetails?: boolean;
}

/**
 * Error Fallback UI Component
 *
 * Displays a user-friendly error message with recovery options.
 * Shows detailed error info in development mode.
 *
 * @example
 * ```tsx
 * <ErrorFallback
 *   error={error}
 *   resetError={resetError}
 * />
 * ```
 */
export function ErrorFallback({ error, resetError, showDetails }: ErrorFallbackProps) {
  const navigate = useNavigate();
  const isDevelopment = import.meta.env.DEV;
  const shouldShowDetails = showDetails ?? isDevelopment;

  const handleGoHome = () => {
    resetError();
    navigate('/');
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 dark:bg-gray-900 px-4 py-16">
      <div className="w-full max-w-md">
        {/* Error Icon */}
        <div className="mb-6 flex justify-center">
          <div className="rounded-full bg-red-100 dark:bg-red-900/20 p-4">
            <AlertTriangle className="h-12 w-12 text-red-600 dark:text-red-400" />
          </div>
        </div>

        {/* Error Message */}
        <div className="text-center mb-6">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
            Algo deu errado
          </h1>
          <p className="text-gray-600 dark:text-gray-400">
            Encontramos um problema inesperado. Você pode tentar novamente ou voltar para a página
            inicial.
          </p>
        </div>

        {/* Error Details (Development Only) */}
        {shouldShowDetails && (
          <div className="mb-6 rounded-lg border border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/10 p-4">
            <h2 className="text-sm font-semibold text-red-800 dark:text-red-400 mb-2">
              Detalhes do Erro (Apenas Desenvolvimento)
            </h2>
            <div className="text-xs text-red-700 dark:text-red-300 font-mono space-y-1">
              <p className="font-semibold">{error.name}: {error.message}</p>
              {error.stack && (
                <details className="mt-2">
                  <summary className="cursor-pointer hover:text-red-800 dark:hover:text-red-200">
                    Stack Trace
                  </summary>
                  <pre className="mt-2 whitespace-pre-wrap text-xs overflow-x-auto">
                    {error.stack}
                  </pre>
                </details>
              )}
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex flex-col gap-3">
          {/* Try Again Button */}
          <button
            onClick={resetError}
            className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-3 text-white font-medium hover:bg-blue-700 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-900"
          >
            <RefreshCw className="h-5 w-5" />
            Tentar Novamente
          </button>

          {/* Go Home Button */}
          <button
            onClick={handleGoHome}
            className="flex items-center justify-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-4 py-3 text-gray-700 dark:text-gray-300 font-medium hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 dark:focus:ring-offset-gray-900"
          >
            <Home className="h-5 w-5" />
            Voltar para Início
          </button>
        </div>

        {/* Help Text */}
        <p className="mt-6 text-center text-xs text-gray-500 dark:text-gray-500">
          Se o problema persistir, entre em contato com o suporte técnico.
        </p>
      </div>
    </div>
  );
}
