import { useState, useEffect, ReactNode } from 'react';
import { RefreshCw, CheckCircle } from 'lucide-react';

interface Button3DRetroProps {
  /** Função a ser executada ao clicar (pode ser async) */
  onClick: () => Promise<void> | void;
  /** Texto do botão no estado normal */
  children?: ReactNode;
  /** Texto alternativo (se não usar children) */
  label?: string;
  /** Mostrar toast de sucesso após completar? (default: true) */
  showToast?: boolean;
  /** Mensagem do toast (default: "Atualizado!") */
  toastMessage?: string;
  /** Delay mínimo em ms para animação (default: 600) */
  minDelay?: number;
  /** Desabilitar botão externamente */
  disabled?: boolean;
  /** Classes CSS adicionais */
  className?: string;
  /** Cor do botão: 'blue' | 'green' | 'red' | 'purple' (default: 'blue') */
  color?: 'blue' | 'green' | 'red' | 'purple';
}

const colorStyles = {
  blue: {
    bg: 'bg-blue-500 hover:bg-blue-600 dark:bg-blue-600 dark:hover:bg-blue-500',
    border: '#1d4ed8',
    borderDark: '#1e40af',
  },
  green: {
    bg: 'bg-green-500 hover:bg-green-600 dark:bg-green-600 dark:hover:bg-green-500',
    border: '#15803d',
    borderDark: '#166534',
  },
  red: {
    bg: 'bg-red-500 hover:bg-red-600 dark:bg-red-600 dark:hover:bg-red-500',
    border: '#b91c1c',
    borderDark: '#991b1b',
  },
  purple: {
    bg: 'bg-purple-500 hover:bg-purple-600 dark:bg-purple-600 dark:hover:bg-purple-500',
    border: '#7c3aed',
    borderDark: '#6d28d9',
  },
};

/**
 * Botão estilo 3D Retro com animação de loading e toast de sucesso.
 *
 * @example
 * ```tsx
 * <Button3DRetro onClick={() => refetch()} label="Recarregar" />
 *
 * <Button3DRetro
 *   onClick={handleSave}
 *   color="green"
 *   toastMessage="Salvo com sucesso!"
 * >
 *   <Save className="h-4 w-4 mr-2" />
 *   Salvar
 * </Button3DRetro>
 * ```
 */
export function Button3DRetro({
  onClick,
  children,
  label = 'Recarregar',
  showToast = true,
  toastMessage = 'Atualizado!',
  minDelay = 600,
  disabled = false,
  className = '',
  color = 'blue',
}: Button3DRetroProps) {
  const [isLoading, setIsLoading] = useState(false);
  const [toastVisible, setToastVisible] = useState(false);

  const styles = colorStyles[color];

  const handleClick = async () => {
    if (isLoading || disabled) return;

    setIsLoading(true);

    try {
      // Executa ação e delay mínimo em paralelo
      await Promise.all([
        Promise.resolve(onClick()),
        new Promise(resolve => setTimeout(resolve, minDelay))
      ]);

      if (showToast) {
        setToastVisible(true);
      }
    } finally {
      setIsLoading(false);
    }
  };

  // Auto-hide toast
  useEffect(() => {
    if (toastVisible) {
      const timer = setTimeout(() => setToastVisible(false), 3000);
      return () => clearTimeout(timer);
    }
  }, [toastVisible]);

  return (
    <>
      {/* Toast Notification */}
      {showToast && (
        <div
          className={`fixed bottom-5 left-1/2 transform -translate-x-1/2 z-50 transition-all duration-300 ease-out ${
            toastVisible ? 'translate-y-0 opacity-100' : 'translate-y-20 opacity-0 pointer-events-none'
          }`}
        >
          <div className="flex items-center gap-2 bg-green-500 dark:bg-green-600 text-white px-6 py-3 rounded-full shadow-2xl">
            <CheckCircle className="h-5 w-5" />
            <span className="font-medium">{toastMessage}</span>
          </div>
        </div>
      )}

      {/* Button */}
      <button
        onClick={handleClick}
        disabled={isLoading || disabled}
        className={`btn-3d-retro flex items-center justify-center ${styles.bg} text-white font-bold uppercase tracking-wide text-sm rounded-lg transition-all duration-100 ${
          isLoading
            ? 'px-4 py-3 pointer-events-none !border-b-0 !translate-y-[6px] !mb-[6px]'
            : 'px-6 sm:px-10 py-3'
        } ${disabled ? 'opacity-50 cursor-not-allowed' : ''} ${className}`}
        style={{
          borderBottomColor: styles.border,
        }}
      >
        {isLoading ? (
          <RefreshCw className="h-5 w-5 animate-spin" />
        ) : (
          children || (
            <>
              <RefreshCw className="h-4 w-4 mr-2" />
              <span>{label}</span>
            </>
          )
        )}
      </button>
    </>
  );
}

export default Button3DRetro;
