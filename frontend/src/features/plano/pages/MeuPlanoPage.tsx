/**
 * Meu Plano Page - Shows the workshop's current plan and features
 */

import { Crown, Check, X, ArrowUpRight, Users, FileText, Clock, Sparkles } from 'lucide-react';
import { useMeuPlano } from '../hooks/usePlano';
import type { PlanoInfo, FeatureInfo } from '../types';

const formatCurrency = (value: number | null | undefined) => {
  if (value == null || value === 0) return 'Sob consulta';
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const formatDate = (date: string | null) => {
  if (!date) return '-';
  return new Intl.DateTimeFormat('pt-BR').format(new Date(date));
};

const StatusBadge = ({ status }: { status: string | null }) => {
  const styles: Record<string, string> = {
    ATIVA: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
    TRIAL: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
    SUSPENSA: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
    INATIVA: 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400',
    CANCELADA: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
  };

  const labels: Record<string, string> = {
    ATIVA: 'Ativa',
    TRIAL: 'Trial',
    SUSPENSA: 'Suspensa',
    INATIVA: 'Inativa',
    CANCELADA: 'Cancelada',
  };

  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${styles[status || 'INATIVA']}`}>
      {labels[status || 'INATIVA']}
    </span>
  );
};

const PlanCard = ({ plano, isCurrentPlan = false }: { plano: PlanoInfo; isCurrentPlan?: boolean }) => {
  const planColors: Record<string, { bg: string; border: string; icon: string }> = {
    ECONOMICO: {
      bg: 'bg-gray-50 dark:bg-gray-800',
      border: 'border-gray-200 dark:border-gray-700',
      icon: 'text-gray-500',
    },
    PROFISSIONAL: {
      bg: 'bg-blue-50 dark:bg-blue-900/20',
      border: 'border-blue-200 dark:border-blue-800',
      icon: 'text-blue-500',
    },
    TURBINADO: {
      bg: 'bg-amber-50 dark:bg-amber-900/20',
      border: 'border-amber-200 dark:border-amber-800',
      icon: 'text-amber-500',
    },
  };

  const colors = planColors[plano.codigo] || planColors.ECONOMICO;

  return (
    <div className={`rounded-xl border-2 ${colors.border} ${colors.bg} p-6 ${isCurrentPlan ? 'ring-2 ring-offset-2 ring-blue-500' : ''}`}>
      {isCurrentPlan && (
        <span className="mb-2 inline-block rounded-full bg-blue-600 px-3 py-1 text-xs font-medium text-white">
          Seu Plano Atual
        </span>
      )}
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-2">
            <Crown className={`h-6 w-6 ${colors.icon}`} />
            <h3 className="text-xl font-bold text-gray-900 dark:text-white">{plano.nome}</h3>
          </div>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">{plano.descricao}</p>
        </div>
        {isCurrentPlan && plano.status && <StatusBadge status={plano.status} />}
      </div>

      <div className="mt-4 flex items-baseline gap-1">
        <span className="text-3xl font-bold text-gray-900 dark:text-white">
          {formatCurrency(plano.valorMensal)}
        </span>
        {plano.valorMensal > 0 && <span className="text-gray-500">/mes</span>}
      </div>

      {isCurrentPlan && plano.diasRestantesTrial !== null && (
        <div className="mt-3 flex items-center gap-2 rounded-lg bg-blue-100 px-3 py-2 text-sm text-blue-800 dark:bg-blue-900/30 dark:text-blue-300">
          <Clock className="h-4 w-4" />
          {plano.diasRestantesTrial} dias restantes no trial
        </div>
      )}

      {isCurrentPlan && plano.dataVencimento && (
        <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
          Vencimento: {formatDate(plano.dataVencimento)}
        </p>
      )}

      {/* Plan Limits */}
      <div className="mt-4 space-y-2 border-t border-gray-200 pt-4 dark:border-gray-700">
        <div className="flex items-center justify-between text-sm">
          <span className="flex items-center gap-2 text-gray-600 dark:text-gray-400">
            <Users className="h-4 w-4" />
            Usuarios
          </span>
          <span className="font-medium text-gray-900 dark:text-white">
            {plano.usuariosIlimitados ? 'Ilimitados' : plano.maxUsuarios}
          </span>
        </div>
        <div className="flex items-center justify-between text-sm">
          <span className="flex items-center gap-2 text-gray-600 dark:text-gray-400">
            <FileText className="h-4 w-4" />
            Ordens de Servico
          </span>
          <span className="font-medium text-gray-900 dark:text-white">
            {plano.maxOrdensServico === -1 ? 'Ilimitadas' : plano.maxOrdensServico}
          </span>
        </div>
        <div className="flex items-center justify-between text-sm">
          <span className="flex items-center gap-2 text-gray-600 dark:text-gray-400">
            <Users className="h-4 w-4" />
            Clientes
          </span>
          <span className="font-medium text-gray-900 dark:text-white">
            {plano.maxClientes === -1 ? 'Ilimitados' : plano.maxClientes}
          </span>
        </div>
      </div>

      {/* Plan Features */}
      <div className="mt-4 space-y-2 border-t border-gray-200 pt-4 dark:border-gray-700">
        <FeatureItem label="Emissao de Nota Fiscal" enabled={plano.emiteNotaFiscal} />
        <FeatureItem label="WhatsApp Automatizado" enabled={plano.whatsappAutomatizado} />
        <FeatureItem label="Manutencao Preventiva" enabled={plano.manutencaoPreventiva} />
        <FeatureItem label="Anexo de Imagens/Docs" enabled={plano.anexoImagensDocumentos} />
      </div>
    </div>
  );
};

const FeatureItem = ({ label, enabled }: { label: string; enabled: boolean }) => (
  <div className="flex items-center gap-2 text-sm">
    {enabled ? (
      <Check className="h-4 w-4 text-green-500" />
    ) : (
      <X className="h-4 w-4 text-gray-400" />
    )}
    <span className={enabled ? 'text-gray-900 dark:text-white' : 'text-gray-400'}>
      {label}
    </span>
  </div>
);

const FeatureCategory = ({ categoria, features }: { categoria: string; features: FeatureInfo[] }) => {
  if (features.length === 0) return null;

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-800">
      <h4 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400">
        {categoria}
      </h4>
      <div className="space-y-2">
        {features.map((feature) => (
          <div key={feature.codigo} className="flex items-start gap-3">
            <div className="mt-0.5 rounded-full bg-green-100 p-1 dark:bg-green-900/30">
              <Check className="h-3 w-3 text-green-600 dark:text-green-400" />
            </div>
            <div>
              <p className="font-medium text-gray-900 dark:text-white">{feature.nome}</p>
              {feature.descricao && (
                <p className="text-sm text-gray-500 dark:text-gray-400">{feature.descricao}</p>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

const UpgradeFeature = ({ feature }: { feature: FeatureInfo }) => (
  <div className="flex items-start gap-3 rounded-lg border border-amber-200 bg-amber-50 p-3 dark:border-amber-800 dark:bg-amber-900/20">
    <div className="mt-0.5 rounded-full bg-amber-100 p-1 dark:bg-amber-900/50">
      <Sparkles className="h-3 w-3 text-amber-600 dark:text-amber-400" />
    </div>
    <div>
      <p className="font-medium text-gray-900 dark:text-white">{feature.nome}</p>
      {feature.descricao && (
        <p className="text-sm text-gray-500 dark:text-gray-400">{feature.descricao}</p>
      )}
      <p className="mt-1 text-xs font-medium text-amber-600 dark:text-amber-400">
        Disponivel no plano {feature.disponivelNoPlano}
      </p>
    </div>
  </div>
);

export const MeuPlanoPage = () => {
  const { data: meuPlano, isLoading, error } = useMeuPlano();

  if (isLoading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <div className="h-12 w-12 animate-spin rounded-full border-4 border-blue-600 border-t-transparent" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="mx-auto max-w-2xl p-6">
        <div className="rounded-lg border border-red-300 bg-red-50 p-4 text-red-800 dark:border-red-800 dark:bg-red-900/20 dark:text-red-400">
          Erro ao carregar informacoes do plano. Tente novamente mais tarde.
        </div>
      </div>
    );
  }

  if (!meuPlano) {
    return (
      <div className="mx-auto max-w-2xl p-6">
        <div className="rounded-lg border border-gray-300 bg-gray-50 p-4 text-gray-600 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400">
          Nenhuma informacao de plano disponivel.
        </div>
      </div>
    );
  }

  // Group features by category
  const featuresByCategory = meuPlano.featuresHabilitadas.reduce(
    (acc, feature) => {
      const categoria = feature.categoria || 'GERAL';
      if (!acc[categoria]) acc[categoria] = [];
      acc[categoria].push(feature);
      return acc;
    },
    {} as Record<string, FeatureInfo[]>
  );

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Meu Plano</h1>
        <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
          Visualize seu plano atual e funcionalidades disponiveis
        </p>
      </div>

      {/* All Plans Comparison */}
      <div className="mb-8">
        <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
          Comparativo de Planos
        </h2>
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {meuPlano.todosPlanos.map((plano) => {
            const isCurrentPlan = plano.codigo === meuPlano.planoAtual.codigo;
            const isUpgrade = !isCurrentPlan &&
              (meuPlano.planoAtual.codigo === 'ECONOMICO' ||
               (meuPlano.planoAtual.codigo === 'PROFISSIONAL' && plano.codigo === 'TURBINADO'));

            return (
              <div key={plano.codigo} className="relative">
                {isUpgrade && (
                  <div className="absolute -top-3 right-4 z-10">
                    <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-3 py-1 text-xs font-medium text-amber-800 dark:bg-amber-900/50 dark:text-amber-300">
                      <ArrowUpRight className="h-3 w-3" />
                      Upgrade
                    </span>
                  </div>
                )}
                <PlanCard
                  plano={isCurrentPlan ? meuPlano.planoAtual : plano}
                  isCurrentPlan={isCurrentPlan}
                />
              </div>
            );
          })}
        </div>
      </div>

      {/* Features Summary */}
      <div className="mb-6 rounded-lg bg-blue-50 p-4 dark:bg-blue-900/20">
        <div className="flex items-center gap-4">
          <div className="rounded-full bg-blue-100 p-3 dark:bg-blue-900/50">
            <Sparkles className="h-6 w-6 text-blue-600 dark:text-blue-400" />
          </div>
          <div>
            <p className="font-semibold text-gray-900 dark:text-white">
              {meuPlano.totalFeaturesHabilitadas} de {meuPlano.totalFeaturesDisponiveis} funcionalidades habilitadas
            </p>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Seu plano {meuPlano.planoAtual.nome} inclui todas as funcionalidades abaixo
            </p>
          </div>
        </div>
      </div>

      {/* Enabled Features by Category */}
      <div className="mb-8">
        <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
          Funcionalidades Habilitadas
        </h2>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {Object.entries(featuresByCategory).map(([categoria, features]) => (
            <FeatureCategory key={categoria} categoria={categoria} features={features} />
          ))}
        </div>
      </div>

      {/* Upgrade Features (Upsell) */}
      {meuPlano.featuresProximoPlano.length > 0 && (
        <div>
          <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900 dark:text-white">
            <ArrowUpRight className="h-5 w-5 text-amber-500" />
            Funcionalidades do Proximo Plano
          </h2>
          <p className="mb-4 text-sm text-gray-600 dark:text-gray-400">
            Faca upgrade para {meuPlano.proximoPlano?.nome} e desbloqueie estas funcionalidades:
          </p>
          <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
            {meuPlano.featuresProximoPlano.map((feature) => (
              <UpgradeFeature key={feature.codigo} feature={feature} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
