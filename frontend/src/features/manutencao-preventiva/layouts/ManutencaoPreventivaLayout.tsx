import { Outlet } from 'react-router-dom';
import { FeatureGate } from '@/shared/components/FeatureGate';
import { PlanUpgradePrompt } from '@/shared/components/PlanUpgradePrompt';

/**
 * Layout wrapper para todas as rotas de Manutenção Preventiva.
 * Verifica se a feature flag MANUTENCAO_PREVENTIVA está habilitada
 * antes de renderizar as páginas filhas.
 */
export function ManutencaoPreventivaLayout() {
  return (
    <FeatureGate
      feature="MANUTENCAO_PREVENTIVA"
      fallback={
        <div className="p-4 sm:p-6 lg:p-8">
          <PlanUpgradePrompt
            featureCode="MANUTENCAO_PREVENTIVA"
            featureName="Manutenção Preventiva"
            requiredPlan="PROFISSIONAL"
            message="O módulo de Manutenção Preventiva permite criar planos de manutenção, agendar revisões e receber alertas automáticos quando seus veículos precisam de serviços. Disponível nos planos Profissional e Turbinado."
          />
        </div>
      }
    >
      <Outlet />
    </FeatureGate>
  );
}
