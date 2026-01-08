/**
 * Componente de Diagnóstico Assistido por IA
 *
 * Usado na tela de Ordem de Serviço para gerar diagnósticos automáticos
 * baseados no problema relatado.
 */

import { useState } from 'react';
import {
  Bot,
  Loader2,
  AlertTriangle,
  ChevronDown,
  ChevronUp,
  Wrench,
  Package,
  Clock,
  DollarSign,
  Zap,
  Info,
} from 'lucide-react';
import { useIADisponivel, useGerarDiagnostico } from '../hooks/useIA';
import type {
  DiagnosticoIAResponse,
  CausaPossivel,
  PecaProvavel,
  Gravidade,
  Urgencia,
  OrigemDiagnostico,
} from '../types';

interface DiagnosticoIAProps {
  veiculoId?: string;
  problemasRelatados?: string;
  onDiagnosticoGerado?: (diagnostico: DiagnosticoIAResponse) => void;
  onUsarDiagnostico?: (diagnostico: string) => void;
}

export const DiagnosticoIA = ({
  veiculoId,
  problemasRelatados,
  onDiagnosticoGerado,
  onUsarDiagnostico,
}: DiagnosticoIAProps) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [diagnostico, setDiagnostico] = useState<DiagnosticoIAResponse | null>(null);

  // Hooks
  const { data: disponivel, isLoading: isLoadingDisponivel } = useIADisponivel();
  const gerarDiagnostico = useGerarDiagnostico();

  // Handlers
  const handleGerarDiagnostico = async () => {
    if (!veiculoId || !problemasRelatados?.trim()) {
      return;
    }

    try {
      const result = await gerarDiagnostico.mutateAsync({
        veiculoId,
        problemasRelatados,
      });
      setDiagnostico(result);
      setIsExpanded(true);
      onDiagnosticoGerado?.(result);
    } catch (error) {
      console.error('Erro ao gerar diagnóstico:', error);
    }
  };

  const handleUsarDiagnostico = () => {
    if (diagnostico?.resumo) {
      onUsarDiagnostico?.(diagnostico.resumo);
    }
  };

  // Se IA não está disponível ou carregando, mostra estado apropriado
  if (isLoadingDisponivel) {
    return (
      <div className="flex items-center space-x-2 text-sm text-gray-500">
        <Loader2 className="h-4 w-4 animate-spin" />
        <span>Verificando disponibilidade da IA...</span>
      </div>
    );
  }

  if (!disponivel) {
    return (
      <div className="rounded-lg border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-800">
        <div className="flex items-center space-x-2 text-sm text-gray-500 dark:text-gray-400">
          <Bot className="h-5 w-5" />
          <span>
            Diagnóstico por IA não disponível.{' '}
            <a href="/configuracoes" className="text-blue-600 hover:underline dark:text-blue-400">
              Configure nas configurações
            </a>
          </span>
        </div>
      </div>
    );
  }

  // Validação para habilitar o botão
  const canGenerate = veiculoId && problemasRelatados?.trim() && problemasRelatados.length >= 10;

  return (
    <div className="rounded-lg border border-blue-200 bg-blue-50/50 dark:border-blue-800 dark:bg-blue-900/20">
      {/* Header */}
      <div className="flex items-center justify-between p-4">
        <div className="flex items-center space-x-2">
          <Bot className="h-5 w-5 text-blue-600 dark:text-blue-400" />
          <span className="font-medium text-blue-700 dark:text-blue-300">
            Diagnóstico Assistido por IA
          </span>
        </div>
        <div className="flex items-center space-x-2">
          {diagnostico && (
            <button
              type="button"
              onClick={() => setIsExpanded(!isExpanded)}
              className="p-1 text-blue-600 hover:text-blue-700 dark:text-blue-400"
            >
              {isExpanded ? <ChevronUp className="h-5 w-5" /> : <ChevronDown className="h-5 w-5" />}
            </button>
          )}
          <button
            type="button"
            onClick={handleGerarDiagnostico}
            disabled={!canGenerate || gerarDiagnostico.isPending}
            className="flex items-center space-x-2 rounded-lg bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            title={
              !canGenerate
                ? 'Selecione um veículo e descreva o problema (mín. 10 caracteres)'
                : 'Gerar diagnóstico com IA'
            }
          >
            {gerarDiagnostico.isPending ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                <span>Analisando...</span>
              </>
            ) : (
              <>
                <Zap className="h-4 w-4" />
                <span>{diagnostico ? 'Gerar Novo' : 'Gerar Diagnóstico'}</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Erro */}
      {gerarDiagnostico.isError && (
        <div className="border-t border-red-200 bg-red-50 px-4 py-3 dark:border-red-800 dark:bg-red-900/20">
          <div className="flex items-center space-x-2 text-red-600 dark:text-red-400">
            <AlertTriangle className="h-4 w-4" />
            <span className="text-sm">
              Erro ao gerar diagnóstico. Verifique a configuração da API key.
            </span>
          </div>
        </div>
      )}

      {/* Resultado */}
      {diagnostico && isExpanded && (
        <div className="border-t border-blue-200 p-4 dark:border-blue-800">
          {/* Origem do diagnóstico */}
          {diagnostico.metadados && (
            <div className="mb-4 flex items-center justify-between">
              <OrigemBadge origem={diagnostico.metadados.origem} />
              <span className="text-xs text-gray-500">
                {diagnostico.metadados.tempoProcessamentoMs}ms
              </span>
            </div>
          )}

          {/* Resumo */}
          <div className="mb-4">
            <h4 className="mb-2 flex items-center font-medium text-gray-900 dark:text-white">
              <Info className="mr-2 h-4 w-4" />
              Resumo
            </h4>
            <p className="rounded-lg bg-white p-3 text-sm text-gray-700 dark:bg-gray-800 dark:text-gray-300">
              {diagnostico.resumo}
            </p>
            {onUsarDiagnostico && (
              <button
                type="button"
                onClick={handleUsarDiagnostico}
                className="mt-2 text-sm text-blue-600 hover:underline dark:text-blue-400"
              >
                Usar como diagnóstico
              </button>
            )}
          </div>

          {/* Causas Possíveis */}
          {diagnostico.causasPossiveis && diagnostico.causasPossiveis.length > 0 && (
            <div className="mb-4">
              <h4 className="mb-2 flex items-center font-medium text-gray-900 dark:text-white">
                <AlertTriangle className="mr-2 h-4 w-4" />
                Causas Possíveis
              </h4>
              <div className="space-y-2">
                {diagnostico.causasPossiveis.map((causa, index) => (
                  <CausaCard key={index} causa={causa} />
                ))}
              </div>
            </div>
          )}

          {/* Ações Recomendadas */}
          {diagnostico.acoesRecomendadas && diagnostico.acoesRecomendadas.length > 0 && (
            <div className="mb-4">
              <h4 className="mb-2 flex items-center font-medium text-gray-900 dark:text-white">
                <Wrench className="mr-2 h-4 w-4" />
                Ações Recomendadas
              </h4>
              <ul className="list-inside list-decimal space-y-1 rounded-lg bg-white p-3 text-sm dark:bg-gray-800">
                {diagnostico.acoesRecomendadas.map((acao, index) => (
                  <li key={index} className="text-gray-700 dark:text-gray-300">
                    {acao}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Peças Prováveis */}
          {diagnostico.pecasProvaveis && diagnostico.pecasProvaveis.length > 0 && (
            <div className="mb-4">
              <h4 className="mb-2 flex items-center font-medium text-gray-900 dark:text-white">
                <Package className="mr-2 h-4 w-4" />
                Peças Prováveis
              </h4>
              <div className="space-y-2">
                {diagnostico.pecasProvaveis.map((peca, index) => (
                  <PecaCard key={index} peca={peca} />
                ))}
              </div>
            </div>
          )}

          {/* Estimativas */}
          <div className="grid grid-cols-2 gap-4">
            <div className="rounded-lg bg-white p-3 dark:bg-gray-800">
              <div className="flex items-center text-gray-500 dark:text-gray-400">
                <Clock className="mr-2 h-4 w-4" />
                <span className="text-xs">Tempo Estimado</span>
              </div>
              <p className="mt-1 font-medium text-gray-900 dark:text-white">
                {diagnostico.estimativaTempo}
              </p>
            </div>
            <div className="rounded-lg bg-white p-3 dark:bg-gray-800">
              <div className="flex items-center text-gray-500 dark:text-gray-400">
                <DollarSign className="mr-2 h-4 w-4" />
                <span className="text-xs">Custo Estimado</span>
              </div>
              <p className="mt-1 font-medium text-gray-900 dark:text-white">
                {diagnostico.faixaCusto && diagnostico.faixaCusto.minimo > 0 ? (
                  <>
                    R$ {diagnostico.faixaCusto.minimo.toFixed(0)} - R${' '}
                    {diagnostico.faixaCusto.maximo.toFixed(0)}
                  </>
                ) : (
                  'A definir'
                )}
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// ===== Componentes Auxiliares =====

const OrigemBadge = ({ origem }: { origem: OrigemDiagnostico }) => {
  const configs: Record<OrigemDiagnostico, { label: string; color: string }> = {
    TEMPLATE: { label: 'Template', color: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300' },
    CACHE: { label: 'Cache', color: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300' },
    IA_HAIKU: { label: 'Claude Haiku', color: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300' },
    IA_SONNET: { label: 'Claude Sonnet', color: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300' },
  };

  const config = configs[origem];

  return (
    <span className={`rounded-full px-2 py-1 text-xs font-medium ${config.color}`}>
      {config.label}
    </span>
  );
};

const CausaCard = ({ causa }: { causa: CausaPossivel }) => {
  const gravidadeColors: Record<Gravidade, string> = {
    BAIXA: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300',
    MEDIA: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-300',
    ALTA: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300',
    CRITICA: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300',
  };

  return (
    <div className="flex items-center justify-between rounded-lg bg-white p-3 dark:bg-gray-800">
      <div className="flex-1">
        <p className="text-sm text-gray-700 dark:text-gray-300">{causa.descricao}</p>
      </div>
      <div className="ml-4 flex items-center space-x-2">
        <span className="text-sm font-medium text-gray-600 dark:text-gray-400">
          {causa.probabilidade}%
        </span>
        <span className={`rounded-full px-2 py-0.5 text-xs ${gravidadeColors[causa.gravidade]}`}>
          {causa.gravidade}
        </span>
      </div>
    </div>
  );
};

const PecaCard = ({ peca }: { peca: PecaProvavel }) => {
  const urgenciaColors: Record<Urgencia, string> = {
    BAIXA: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300',
    MEDIA: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-300',
    ALTA: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300',
    IMEDIATA: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300',
  };

  return (
    <div className="flex items-center justify-between rounded-lg bg-white p-3 dark:bg-gray-800">
      <div>
        <p className="text-sm font-medium text-gray-700 dark:text-gray-300">{peca.nome}</p>
        {peca.codigoReferencia && (
          <p className="text-xs text-gray-500">{peca.codigoReferencia}</p>
        )}
      </div>
      <div className="flex items-center space-x-2">
        {peca.custoEstimado > 0 && (
          <span className="text-sm text-gray-600 dark:text-gray-400">
            ~R$ {peca.custoEstimado.toFixed(0)}
          </span>
        )}
        <span className={`rounded-full px-2 py-0.5 text-xs ${urgenciaColors[peca.urgencia]}`}>
          {peca.urgencia}
        </span>
      </div>
    </div>
  );
};
