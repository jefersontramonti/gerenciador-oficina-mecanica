import { Link } from 'react-router-dom';
import { Lock, Sparkles, ArrowRight } from 'lucide-react';

interface PlanUpgradePromptProps {
  /**
   * Código da feature que está sendo bloqueada.
   */
  featureCode?: string;

  /**
   * Nome amigável da feature (para exibição).
   */
  featureName?: string;

  /**
   * Plano necessário para ter acesso.
   */
  requiredPlan?: 'ECONOMICO' | 'PROFISSIONAL' | 'TURBINADO';

  /**
   * Mensagem personalizada a exibir.
   */
  message?: string;

  /**
   * Variante de exibição:
   * - "card": Card grande com mais detalhes (padrão)
   * - "inline": Versão compacta para uso inline
   * - "banner": Banner horizontal
   */
  variant?: 'card' | 'inline' | 'banner';

  /**
   * Se deve mostrar o botão de upgrade.
   */
  showUpgradeButton?: boolean;

  /**
   * Classe CSS adicional.
   */
  className?: string;
}

// Nomes amigáveis dos planos
const PLAN_NAMES: Record<string, string> = {
  ECONOMICO: 'Econômico',
  PROFISSIONAL: 'Profissional',
  TURBINADO: 'Turbinado',
};

// Cores por plano
const PLAN_COLORS: Record<string, { bg: string; text: string; border: string }> = {
  ECONOMICO: {
    bg: 'bg-blue-100 dark:bg-blue-900/30',
    text: 'text-blue-700 dark:text-blue-400',
    border: 'border-blue-200 dark:border-blue-800',
  },
  PROFISSIONAL: {
    bg: 'bg-purple-100 dark:bg-purple-900/30',
    text: 'text-purple-700 dark:text-purple-400',
    border: 'border-purple-200 dark:border-purple-800',
  },
  TURBINADO: {
    bg: 'bg-amber-100 dark:bg-amber-900/30',
    text: 'text-amber-700 dark:text-amber-400',
    border: 'border-amber-200 dark:border-amber-800',
  },
};

/**
 * Componente para exibir mensagem de upgrade quando uma feature não está disponível.
 *
 * @example Card variant (default)
 * ```tsx
 * <PlanUpgradePrompt
 *   featureName="Emissão de NF-e"
 *   requiredPlan="PROFISSIONAL"
 * />
 * ```
 *
 * @example Inline variant
 * ```tsx
 * <PlanUpgradePrompt
 *   featureName="Relatórios Gerenciais"
 *   requiredPlan="PROFISSIONAL"
 *   variant="inline"
 * />
 * ```
 *
 * @example Banner variant
 * ```tsx
 * <PlanUpgradePrompt
 *   message="Aproveite todas as funcionalidades!"
 *   requiredPlan="TURBINADO"
 *   variant="banner"
 * />
 * ```
 */
export function PlanUpgradePrompt({
  featureCode,
  featureName,
  requiredPlan = 'PROFISSIONAL',
  message,
  variant = 'card',
  showUpgradeButton = true,
  className = '',
}: PlanUpgradePromptProps) {
  const planName = PLAN_NAMES[requiredPlan] || requiredPlan;
  const colors = PLAN_COLORS[requiredPlan] || PLAN_COLORS.PROFISSIONAL;

  const defaultMessage = featureName
    ? `A funcionalidade "${featureName}" está disponível no plano ${planName} ou superior.`
    : `Esta funcionalidade está disponível no plano ${planName} ou superior.`;

  const displayMessage = message || defaultMessage;

  // Inline variant
  if (variant === 'inline') {
    return (
      <div
        className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full ${colors.bg} ${colors.border} border text-sm ${className}`}
      >
        <Lock className={`h-3.5 w-3.5 ${colors.text}`} />
        <span className={colors.text}>
          Disponível no {planName}
        </span>
        {showUpgradeButton && (
          <Link
            to="/meu-plano"
            className={`${colors.text} hover:underline font-medium`}
          >
            Fazer upgrade
          </Link>
        )}
      </div>
    );
  }

  // Banner variant
  if (variant === 'banner') {
    return (
      <div
        className={`flex flex-col sm:flex-row items-center justify-between gap-3 sm:gap-4 p-3 sm:p-4 rounded-lg ${colors.bg} ${colors.border} border ${className}`}
      >
        <div className="flex items-center gap-3 text-center sm:text-left">
          <div className={`p-2 rounded-full ${colors.bg}`}>
            <Sparkles className={`h-5 w-5 ${colors.text}`} />
          </div>
          <p className={`text-sm ${colors.text}`}>
            {displayMessage}
          </p>
        </div>
        {showUpgradeButton && (
          <Link
            to="/meu-plano"
            className={`flex items-center gap-1.5 px-4 py-2 rounded-lg bg-white dark:bg-gray-800 ${colors.text} border ${colors.border} hover:bg-gray-50 dark:hover:bg-gray-700 text-sm font-medium whitespace-nowrap transition-colors`}
          >
            Fazer upgrade
            <ArrowRight className="h-4 w-4" />
          </Link>
        )}
      </div>
    );
  }

  // Card variant (default)
  return (
    <div
      className={`rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden ${className}`}
    >
      {/* Header */}
      <div className={`px-4 sm:px-6 py-3 ${colors.bg} border-b ${colors.border}`}>
        <div className="flex items-center gap-2">
          <Lock className={`h-5 w-5 ${colors.text}`} />
          <span className={`font-semibold ${colors.text}`}>
            Funcionalidade Premium
          </span>
        </div>
      </div>

      {/* Body */}
      <div className="p-4 sm:p-6">
        <div className="flex flex-col items-center text-center gap-4">
          {/* Icon */}
          <div className={`p-4 rounded-full ${colors.bg}`}>
            <Sparkles className={`h-8 w-8 ${colors.text}`} />
          </div>

          {/* Text */}
          <div className="space-y-2">
            {featureName && (
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                {featureName}
              </h3>
            )}
            <p className="text-sm text-gray-600 dark:text-gray-400 max-w-md">
              {displayMessage}
            </p>
          </div>

          {/* Plan badge */}
          <div className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full ${colors.bg} ${colors.border} border`}>
            <span className={`text-sm font-medium ${colors.text}`}>
              Plano {planName}
            </span>
          </div>

          {/* Upgrade button */}
          {showUpgradeButton && (
            <Link
              to="/meu-plano"
              className="flex items-center gap-2 px-6 py-2.5 rounded-lg bg-blue-600 dark:bg-blue-700 text-white hover:bg-blue-700 dark:hover:bg-blue-600 text-sm font-medium transition-colors"
            >
              <Sparkles className="h-4 w-4" />
              Ver planos e fazer upgrade
            </Link>
          )}
        </div>
      </div>

      {/* Feature code (for debugging) */}
      {featureCode && import.meta.env.DEV && (
        <div className="px-4 sm:px-6 py-2 bg-gray-50 dark:bg-gray-700/50 border-t border-gray-200 dark:border-gray-700">
          <code className="text-xs text-gray-500 dark:text-gray-400">
            Feature: {featureCode}
          </code>
        </div>
      )}
    </div>
  );
}
