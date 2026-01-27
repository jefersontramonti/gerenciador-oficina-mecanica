import { useState } from 'react';
import type { ReactNode, ButtonHTMLAttributes } from 'react';
import { Loader2, CheckCircle, XCircle } from 'lucide-react';

interface ButtonShineProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'onClick'> {
  /** Função a ser executada ao clicar (pode ser async) */
  onClick?: () => Promise<void> | void;
  /** Conteúdo do botão */
  children: ReactNode;
  /** Mostrar estado de loading (controlado externamente) */
  isLoading?: boolean;
  /** Texto durante loading (default: "Aguarde...") */
  loadingText?: string;
  /** Cor do botão: 'blue' | 'green' | 'red' | 'purple' | 'orange' (default: 'blue') */
  color?: 'blue' | 'green' | 'red' | 'purple' | 'orange';
  /** Variante: 'solid' | 'outline' (default: 'solid') */
  variant?: 'solid' | 'outline';
  /** Tamanho: 'sm' | 'md' | 'lg' (default: 'md') */
  size?: 'sm' | 'md' | 'lg';
  /** Ocupar largura total */
  fullWidth?: boolean;
  /** Classes CSS adicionais */
  className?: string;
  /** Mostrar toast de feedback (default: true) */
  showToast?: boolean;
  /** Mensagem de sucesso (default: "Cadastrado com sucesso!") */
  successMessage?: string;
  /** Mensagem de erro (default: "Erro ao cadastrar. Tente novamente.") */
  errorMessage?: string;
  /** Delay mínimo em ms para animação (default: 600) */
  minDelay?: number;
  /** Callback executado quando o toast de sucesso é fechado */
  onSuccess?: () => void;
}

const colorStyles = {
  blue: {
    solid: 'bg-blue-600 hover:bg-blue-700 dark:bg-blue-600 dark:hover:bg-blue-700 text-white',
    outline: 'border-2 border-blue-600 text-blue-600 hover:bg-blue-600 hover:text-white dark:border-blue-500 dark:text-blue-500 dark:hover:bg-blue-500 dark:hover:text-white',
  },
  green: {
    solid: 'bg-green-600 hover:bg-green-700 dark:bg-green-600 dark:hover:bg-green-700 text-white',
    outline: 'border-2 border-green-600 text-green-600 hover:bg-green-600 hover:text-white dark:border-green-500 dark:text-green-500 dark:hover:bg-green-500 dark:hover:text-white',
  },
  red: {
    solid: 'bg-red-600 hover:bg-red-700 dark:bg-red-600 dark:hover:bg-red-700 text-white',
    outline: 'border-2 border-red-600 text-red-600 hover:bg-red-600 hover:text-white dark:border-red-500 dark:text-red-500 dark:hover:bg-red-500 dark:hover:text-white',
  },
  purple: {
    solid: 'bg-purple-600 hover:bg-purple-700 dark:bg-purple-600 dark:hover:bg-purple-700 text-white',
    outline: 'border-2 border-purple-600 text-purple-600 hover:bg-purple-600 hover:text-white dark:border-purple-500 dark:text-purple-500 dark:hover:bg-purple-500 dark:hover:text-white',
  },
  orange: {
    solid: 'bg-orange-500 hover:bg-orange-600 dark:bg-orange-500 dark:hover:bg-orange-600 text-white',
    outline: 'border-2 border-orange-500 text-orange-500 hover:bg-orange-500 hover:text-white dark:border-orange-400 dark:text-orange-400 dark:hover:bg-orange-400 dark:hover:text-white',
  },
};

const sizeStyles = {
  sm: 'px-4 py-2 text-sm',
  md: 'px-6 sm:px-10 py-3 text-sm',
  lg: 'px-8 sm:px-12 py-4 text-base',
};

type ToastState = {
  visible: boolean;
  type: 'success' | 'error';
  message: string;
};

/**
 * Botão com efeito de brilho (shine sweep) no hover.
 * Ideal para ações de cadastro, submit de formulários, CTAs.
 * Inclui feedback visual com toast de sucesso/erro.
 *
 * @example
 * ```tsx
 * // Básico com toast
 * <ButtonShine onClick={handleSubmit}>Cadastrar</ButtonShine>
 *
 * // Com loading controlado externamente
 * <ButtonShine
 *   type="submit"
 *   isLoading={isPending}
 *   successMessage="Cliente cadastrado!"
 * >
 *   Cadastrar
 * </ButtonShine>
 *
 * // Cores e tamanhos
 * <ButtonShine color="green" size="lg">Confirmar</ButtonShine>
 * ```
 */
export function ButtonShine({
  onClick,
  children,
  isLoading = false,
  loadingText = 'Aguarde...',
  color = 'blue',
  variant = 'solid',
  size = 'md',
  fullWidth = false,
  className = '',
  disabled,
  type = 'button',
  showToast = true,
  successMessage = 'Cadastrado com sucesso!',
  errorMessage = 'Erro ao cadastrar. Tente novamente.',
  minDelay = 600,
  onSuccess,
  ...props
}: ButtonShineProps) {
  const [isClicked, setIsClicked] = useState(false);
  const [internalLoading, setInternalLoading] = useState(false);
  const [toast, setToast] = useState<ToastState>({ visible: false, type: 'success', message: '' });

  // Combina loading interno com externo
  const loading = isLoading || internalLoading;

  const showToastMessage = (type: 'success' | 'error', message: string) => {
    setToast({ visible: true, type, message });
  };

  const handleCloseToast = () => {
    const wasSuccess = toast.type === 'success';
    setToast(prev => ({ ...prev, visible: false }));
    // Executa callback de sucesso após fechar o toast
    if (wasSuccess && onSuccess) {
      // Pequeno delay para a animação de saída completar
      setTimeout(() => onSuccess(), 300);
    }
  };

  const handleClick = async () => {
    if (loading || disabled || !onClick) return;

    // Efeito visual de clique
    setIsClicked(true);
    setTimeout(() => setIsClicked(false), 150);

    setInternalLoading(true);

    try {
      // Executa ação e delay mínimo em paralelo
      await Promise.all([
        Promise.resolve(onClick()),
        new Promise(resolve => setTimeout(resolve, minDelay))
      ]);

      if (showToast) {
        showToastMessage('success', successMessage);
      }
    } catch (error) {
      if (showToast) {
        // Usa a mensagem do erro se disponível, senão usa a mensagem padrão
        const message = error instanceof Error && error.message
          ? error.message
          : errorMessage;
        showToastMessage('error', message);
      }
    } finally {
      setInternalLoading(false);
    }
  };

  const isDisabled = loading || disabled;

  return (
    <>
      {/* Toast Notification - Cai de cima para baixo (Responsivo) */}
      {showToast && (
        <div
          className={`fixed inset-0 flex items-start justify-center z-50 pointer-events-none px-4 pt-4 transition-all duration-500 ease-out ${
            toast.visible ? 'opacity-100' : 'opacity-0'
          }`}
        >
          <div
            className={`pointer-events-auto transform transition-all duration-500 ease-out w-full max-w-xs sm:max-w-sm ${
              toast.visible
                ? 'translate-y-[30vh] sm:translate-y-[40vh] scale-100'
                : '-translate-y-full scale-90'
            }`}
          >
            <div className={`
              flex flex-col items-center gap-2 sm:gap-3 px-5 sm:px-8 py-5 sm:py-6 rounded-xl sm:rounded-2xl shadow-2xl backdrop-blur-sm
              border text-center
              ${toast.type === 'success'
                ? 'bg-slate-800/95 border-slate-700'
                : 'bg-slate-800/95 border-red-800'
              }
            `}>
              {/* Ícone */}
              <div className={`
                w-12 h-12 sm:w-16 sm:h-16 rounded-full flex items-center justify-center
                ${toast.type === 'success'
                  ? 'bg-green-500/20 text-green-400'
                  : 'bg-red-500/20 text-red-400'
                }
              `}>
                {toast.type === 'success'
                  ? <CheckCircle className="h-6 w-6 sm:h-8 sm:w-8" />
                  : <XCircle className="h-6 w-6 sm:h-8 sm:w-8" />
                }
              </div>

              {/* Título */}
              <h3 className="text-lg sm:text-xl font-bold text-white">
                {toast.type === 'success' ? 'Sucesso!' : 'Erro!'}
              </h3>

              {/* Mensagem */}
              <p className="text-sm sm:text-base text-gray-400 mb-2 sm:mb-4">
                {toast.message}
              </p>

              {/* Botão OK */}
              <button
                onClick={handleCloseToast}
                className={`
                  w-full py-2 sm:py-2.5 px-6 rounded-lg font-semibold text-sm sm:text-base transition-colors
                  ${toast.type === 'success'
                    ? 'bg-green-600 hover:bg-green-700 active:bg-green-800 text-white'
                    : 'bg-red-600 hover:bg-red-700 active:bg-red-800 text-white'
                  }
                `}
              >
                OK
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Backdrop quando toast está visível */}
      {showToast && (
        <div
          className={`fixed inset-0 bg-black/50 backdrop-blur-sm z-40 transition-opacity duration-300 ${
            toast.visible ? 'opacity-100' : 'opacity-0 pointer-events-none'
          }`}
          onClick={handleCloseToast}
        />
      )}

      {/* Button */}
      <button
        type={type}
        onClick={handleClick}
        disabled={isDisabled}
        className={`
          btn-shine
          ${colorStyles[color][variant]}
          ${sizeStyles[size]}
          ${fullWidth ? 'w-full' : ''}
          rounded-lg shadow-md font-bold tracking-wide
          transition-all duration-200
          flex items-center justify-center gap-2
          ${isClicked ? 'scale-95' : 'scale-100'}
          ${isDisabled ? 'opacity-60 cursor-not-allowed' : 'cursor-pointer'}
          ${className}
        `}
        {...props}
      >
        {loading ? (
          <>
            <Loader2 className="h-4 w-4 animate-spin" />
            <span>{loadingText}</span>
          </>
        ) : (
          children
        )}
      </button>
    </>
  );
}

export default ButtonShine;
