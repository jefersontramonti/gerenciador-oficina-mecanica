import { Moon, Sun } from 'lucide-react';
import { useTheme } from '@/shared/contexts';
import { cn } from '@/shared/utils/cn';

interface ThemeToggleProps {
  /** Tamanho do botão - padrão: 'default' */
  size?: 'small' | 'default' | 'large';
  /** Exibir como ícone apenas (sem fundo) */
  variant?: 'default' | 'ghost';
  /** Classe CSS adicional */
  className?: string;
}

/**
 * ThemeToggle - Botão para alternar entre tema claro e escuro
 *
 * Características:
 * - Animação suave na troca de ícone
 * - Tooltip indicando o tema atual
 * - Persistência da preferência
 * - Responsivo
 */
export function ThemeToggle({ size = 'default', variant = 'default', className }: ThemeToggleProps) {
  const { theme, toggleTheme } = useTheme();

  const sizeClasses = {
    small: 'h-8 w-8',
    default: 'h-9 w-9',
    large: 'h-10 w-10',
  };

  const iconSizes = {
    small: 16,
    default: 18,
    large: 20,
  };

  return (
    <button
      onClick={toggleTheme}
      className={cn(
        'inline-flex items-center justify-center rounded-lg transition-all duration-200',
        variant === 'default' && [
          'bg-gray-100 hover:bg-gray-200',
          'dark:bg-gray-800 dark:hover:bg-gray-700',
        ],
        variant === 'ghost' && [
          'hover:bg-gray-100 dark:hover:bg-gray-800',
        ],
        sizeClasses[size],
        className
      )}
      title={theme === 'light' ? 'Ativar tema escuro' : 'Ativar tema claro'}
      aria-label={theme === 'light' ? 'Ativar tema escuro' : 'Ativar tema claro'}
    >
      {/* Ícone com transição suave */}
      <div className="relative">
        <Sun
          size={iconSizes[size]}
          className={cn(
            'absolute inset-0 transition-all duration-300',
            theme === 'light'
              ? 'rotate-0 scale-100 opacity-100 text-amber-500'
              : 'rotate-90 scale-0 opacity-0'
          )}
        />
        <Moon
          size={iconSizes[size]}
          className={cn(
            'transition-all duration-300',
            theme === 'dark'
              ? 'rotate-0 scale-100 opacity-100 text-blue-400'
              : '-rotate-90 scale-0 opacity-0'
          )}
        />
      </div>
    </button>
  );
}
