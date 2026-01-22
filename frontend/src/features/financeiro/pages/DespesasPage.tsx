/**
 * Página de gerenciamento de despesas operacionais
 */

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import {
  Plus,
  Receipt,
  FilterX,
  CheckCircle,
  XCircle,
  AlertTriangle,
  AlertCircle,
  Clock,
  Trash2,
  Edit,
  RefreshCw,
  ChevronDown,
  ChevronUp,
  Info,
  Lightbulb,
} from 'lucide-react';
import {
  useDespesas,
  useDespesasResumo,
  usePagarDespesa,
  useCancelarDespesa,
  useDeleteDespesa,
} from '../hooks/useDespesas';
import type {
  StatusDespesa,
  CategoriaDespesa,
  TipoPagamento,
  DespesaFiltros,
} from '../types/despesa';
import {
  STATUS_LABELS,
  CATEGORIAS_AGRUPADAS,
  GRUPO_LABELS,
} from '../types/despesa';

const ITEMS_PER_PAGE = 20;

const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const formatDate = (dateString?: string | null): string => {
  if (!dateString) return '-';
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '-';
    return format(date, 'dd/MM/yyyy', { locale: ptBR });
  } catch {
    return '-';
  }
};

// Tipos de alertas de despesas
type NivelAlertaDespesa = 'CRITICAL' | 'WARNING' | 'INFO';

interface AlertaDespesa {
  nivel: NivelAlertaDespesa;
  mensagem: string;
  sugestao?: string;
}

// Função para gerar alertas baseados no resumo
function gerarAlertasDespesas(resumo: {
  totalVencido: number;
  quantidadeVencida: number;
  totalPendente: number;
  quantidadePendente: number;
  quantidadeAVencer7Dias: number;
}): AlertaDespesa[] {
  const alertas: AlertaDespesa[] = [];

  // Alerta crítico: Despesas vencidas
  if (resumo.quantidadeVencida > 0) {
    alertas.push({
      nivel: 'CRITICAL',
      mensagem: `Você tem ${resumo.quantidadeVencida} despesa(s) vencida(s) totalizando ${formatCurrency(resumo.totalVencido)}`,
      sugestao: 'Regularize as despesas vencidas para evitar multas e juros. Priorize os pagamentos mais urgentes.',
    });
  }

  // Alerta de atenção: Muitas despesas a vencer em 7 dias
  if (resumo.quantidadeAVencer7Dias >= 5) {
    alertas.push({
      nivel: 'WARNING',
      mensagem: `${resumo.quantidadeAVencer7Dias} despesas vencem nos próximos 7 dias`,
      sugestao: 'Verifique seu fluxo de caixa para garantir que haverá saldo suficiente para esses pagamentos.',
    });
  } else if (resumo.quantidadeAVencer7Dias >= 3) {
    alertas.push({
      nivel: 'INFO',
      mensagem: `${resumo.quantidadeAVencer7Dias} despesas vencem nos próximos 7 dias`,
    });
  }

  // Alerta de atenção: Total pendente alto
  if (resumo.totalPendente > 10000 && resumo.quantidadePendente > 10) {
    alertas.push({
      nivel: 'WARNING',
      mensagem: `Total de ${formatCurrency(resumo.totalPendente)} em ${resumo.quantidadePendente} despesas pendentes`,
      sugestao: 'Considere negociar prazos ou parcelamentos para melhorar seu fluxo de caixa.',
    });
  }

  return alertas;
}

// Configuração visual dos alertas
function getAlertaConfig(nivel: NivelAlertaDespesa) {
  switch (nivel) {
    case 'CRITICAL':
      return {
        icon: AlertCircle,
        bgColor: 'bg-red-50 dark:bg-red-900/30',
        borderColor: 'border-red-200 dark:border-red-800',
        textColor: 'text-red-800 dark:text-red-200',
        iconColor: 'text-red-600 dark:text-red-400',
        badgeColor: 'bg-red-100 dark:bg-red-800 text-red-800 dark:text-red-200',
        label: 'Crítico',
      };
    case 'WARNING':
      return {
        icon: AlertTriangle,
        bgColor: 'bg-yellow-50 dark:bg-yellow-900/30',
        borderColor: 'border-yellow-200 dark:border-yellow-800',
        textColor: 'text-yellow-800 dark:text-yellow-200',
        iconColor: 'text-yellow-600 dark:text-yellow-400',
        badgeColor: 'bg-yellow-100 dark:bg-yellow-800 text-yellow-800 dark:text-yellow-200',
        label: 'Atenção',
      };
    case 'INFO':
    default:
      return {
        icon: Info,
        bgColor: 'bg-blue-50 dark:bg-blue-900/30',
        borderColor: 'border-blue-200 dark:border-blue-800',
        textColor: 'text-blue-800 dark:text-blue-200',
        iconColor: 'text-blue-600 dark:text-blue-400',
        badgeColor: 'bg-blue-100 dark:bg-blue-800 text-blue-800 dark:text-blue-200',
        label: 'Info',
      };
  }
}

// Badge de status
function StatusBadge({ status, vencida }: { status: StatusDespesa; vencida?: boolean }) {
  const getStatusStyle = () => {
    if (vencida && status === 'PENDENTE') {
      return 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-400';
    }
    switch (status) {
      case 'PAGA':
        return 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-400';
      case 'PENDENTE':
        return 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-400';
      case 'VENCIDA':
        return 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-400';
      case 'CANCELADA':
        return 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-400';
      default:
        return 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-400';
    }
  };

  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${getStatusStyle()}`}>
      {vencida && status === 'PENDENTE' ? 'Vencida' : STATUS_LABELS[status]}
    </span>
  );
}

// Modal de pagamento
function PagarModal({
  isOpen,
  onClose,
  onConfirm,
  isLoading,
}: {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (data: { dataPagamento: string; tipoPagamento: TipoPagamento }) => void;
  isLoading: boolean;
}) {
  const [dataPagamento, setDataPagamento] = useState(new Date().toISOString().split('T')[0]);
  const [tipoPagamento, setTipoPagamento] = useState<TipoPagamento>('PIX');

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-center justify-center p-2 sm:p-4">
      <div className="bg-white dark:bg-gray-800 rounded-lg p-4 sm:p-6 max-w-sm sm:max-w-md w-full shadow-xl">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
          Registrar Pagamento
        </h3>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Data do Pagamento
            </label>
            <input
              type="date"
              value={dataPagamento}
              onChange={(e) => setDataPagamento(e.target.value)}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white px-3 py-2"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Forma de Pagamento
            </label>
            <select
              value={tipoPagamento}
              onChange={(e) => setTipoPagamento(e.target.value as TipoPagamento)}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white px-3 py-2"
            >
              <option value="PIX">PIX</option>
              <option value="DINHEIRO">Dinheiro</option>
              <option value="CARTAO_CREDITO">Cartão de Crédito</option>
              <option value="CARTAO_DEBITO">Cartão de Débito</option>
              <option value="TRANSFERENCIA">Transferência</option>
              <option value="BOLETO">Boleto</option>
            </select>
          </div>
        </div>

        <div className="flex flex-col-reverse sm:flex-row justify-end gap-2 mt-6">
          <button
            onClick={onClose}
            disabled={isLoading}
            className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            Cancelar
          </button>
          <button
            onClick={() => onConfirm({ dataPagamento, tipoPagamento })}
            disabled={isLoading}
            className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 flex items-center justify-center gap-2"
          >
            {isLoading ? (
              <RefreshCw className="h-4 w-4 animate-spin" />
            ) : (
              <CheckCircle className="h-4 w-4" />
            )}
            Confirmar Pagamento
          </button>
        </div>
      </div>
    </div>
  );
}

export function DespesasPage() {
  const [page, setPage] = useState(0);
  const [filtros, setFiltros] = useState<DespesaFiltros>({});
  const [pagarModalId, setPagarModalId] = useState<string | null>(null);
  const [alertasExpandidos, setAlertasExpandidos] = useState(true);

  const { data, isLoading, error, isError } = useDespesas({
    ...filtros,
    page,
    size: ITEMS_PER_PAGE,
  });
  const { data: resumo } = useDespesasResumo();
  const pagarMutation = usePagarDespesa();
  const cancelarMutation = useCancelarDespesa();
  const deleteMutation = useDeleteDespesa();

  // Gerar alertas inteligentes baseados no resumo
  const alertas = resumo ? gerarAlertasDespesas(resumo) : [];
  const alertasCount = {
    critical: alertas.filter((a) => a.nivel === 'CRITICAL').length,
    warning: alertas.filter((a) => a.nivel === 'WARNING').length,
    info: alertas.filter((a) => a.nivel === 'INFO').length,
    total: alertas.length,
  };

  const handlePagar = async (pagamentoData: { dataPagamento: string; tipoPagamento: TipoPagamento }) => {
    if (!pagarModalId) return;
    await pagarMutation.mutateAsync({ id: pagarModalId, data: pagamentoData });
    setPagarModalId(null);
  };

  const handleCancelar = async (id: string) => {
    if (!window.confirm('Cancelar esta despesa?')) return;
    await cancelarMutation.mutateAsync(id);
  };

  const handleExcluir = async (id: string) => {
    if (!window.confirm('Excluir esta despesa?')) return;
    await deleteMutation.mutateAsync(id);
  };

  const handleFiltroChange = (key: keyof DespesaFiltros, value: any) => {
    setFiltros((prev) => ({ ...prev, [key]: value || undefined }));
    setPage(0);
  };

  const limparFiltros = () => {
    setFiltros({});
    setPage(0);
  };

  return (
    <div className="p-4 sm:p-6">
      {/* Error State */}
      {isError && (
        <div className="mb-6 rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400 dark:text-red-300">
          <p className="font-semibold">Erro ao carregar despesas</p>
          <p className="mt-1 text-sm">{error?.message || 'Tente novamente mais tarde.'}</p>
        </div>
      )}

      {/* Header */}
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100">Despesas</h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            Gerencie as despesas operacionais da oficina
          </p>
        </div>
        <Link
          to="/financeiro/despesas/nova"
          className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
        >
          <Plus className="h-5 w-5" />
          <span>Nova Despesa</span>
        </Link>
      </div>

      {/* Resumo Cards */}
      {resumo && (
        <div className="mb-6 grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <div className="flex items-center justify-between gap-2">
              <div className="min-w-0">
                <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400 truncate">
                  Pendentes
                </p>
                <p className="mt-1 sm:mt-2 text-lg sm:text-2xl font-bold text-yellow-600 dark:text-yellow-400">
                  {formatCurrency(resumo.totalPendente)}
                </p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  {resumo.quantidadePendente} despesa(s)
                </p>
              </div>
              <div className="rounded-full bg-yellow-100 dark:bg-yellow-900/30 p-2 sm:p-3 shrink-0">
                <Clock className="h-5 w-5 sm:h-6 sm:w-6 text-yellow-600 dark:text-yellow-400" />
              </div>
            </div>
          </div>

          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <div className="flex items-center justify-between gap-2">
              <div className="min-w-0">
                <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400 truncate">
                  Vencidas
                </p>
                <p className="mt-1 sm:mt-2 text-lg sm:text-2xl font-bold text-red-600 dark:text-red-400">
                  {formatCurrency(resumo.totalVencido)}
                </p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  {resumo.quantidadeVencida} despesa(s)
                </p>
              </div>
              <div className="rounded-full bg-red-100 dark:bg-red-900/30 p-2 sm:p-3 shrink-0">
                <AlertTriangle className="h-5 w-5 sm:h-6 sm:w-6 text-red-600 dark:text-red-400" />
              </div>
            </div>
          </div>

          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <div className="flex items-center justify-between gap-2">
              <div className="min-w-0">
                <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400 truncate">
                  Pago no Mês
                </p>
                <p className="mt-1 sm:mt-2 text-lg sm:text-2xl font-bold text-green-600 dark:text-green-400">
                  {formatCurrency(resumo.totalPagoMes)}
                </p>
              </div>
              <div className="rounded-full bg-green-100 dark:bg-green-900/30 p-2 sm:p-3 shrink-0">
                <CheckCircle className="h-5 w-5 sm:h-6 sm:w-6 text-green-600 dark:text-green-400" />
              </div>
            </div>
          </div>

          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <div className="flex items-center justify-between gap-2">
              <div className="min-w-0">
                <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400 truncate">
                  A Vencer (7 dias)
                </p>
                <p className="mt-1 sm:mt-2 text-lg sm:text-2xl font-bold text-blue-600 dark:text-blue-400">
                  {resumo.quantidadeAVencer7Dias}
                </p>
              </div>
              <div className="rounded-full bg-blue-100 dark:bg-blue-900/30 p-2 sm:p-3 shrink-0">
                <Receipt className="h-5 w-5 sm:h-6 sm:w-6 text-blue-600 dark:text-blue-400" />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Alertas Inteligentes */}
      {alertas.length > 0 && (
        <div className="mb-6 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden shadow">
          <button
            onClick={() => setAlertasExpandidos(!alertasExpandidos)}
            className="w-full p-4 flex items-center justify-between hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
          >
            <div className="flex items-center gap-3">
              <div className="p-2 bg-amber-100 dark:bg-amber-900/30 rounded-lg">
                <AlertTriangle className="h-5 w-5 text-amber-600 dark:text-amber-400" />
              </div>
              <div className="text-left">
                <h3 className="text-base font-semibold text-gray-900 dark:text-white">
                  Alertas Inteligentes
                </h3>
                <div className="flex items-center gap-2 mt-1">
                  {alertasCount.critical > 0 && (
                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-100 dark:bg-red-900/50 text-red-800 dark:text-red-200">
                      {alertasCount.critical} crítico{alertasCount.critical > 1 ? 's' : ''}
                    </span>
                  )}
                  {alertasCount.warning > 0 && (
                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-yellow-100 dark:bg-yellow-900/50 text-yellow-800 dark:text-yellow-200">
                      {alertasCount.warning} atenção
                    </span>
                  )}
                  {alertasCount.info > 0 && (
                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 dark:bg-blue-900/50 text-blue-800 dark:text-blue-200">
                      {alertasCount.info} info
                    </span>
                  )}
                </div>
              </div>
            </div>
            {alertasExpandidos ? (
              <ChevronUp className="h-5 w-5 text-gray-400" />
            ) : (
              <ChevronDown className="h-5 w-5 text-gray-400" />
            )}
          </button>

          {alertasExpandidos && (
            <div className="border-t border-gray-200 dark:border-gray-700 divide-y divide-gray-200 dark:divide-gray-700">
              {alertas.map((alerta, index) => {
                const config = getAlertaConfig(alerta.nivel);
                const IconComponent = config.icon;

                return (
                  <div key={index} className={`p-4 ${config.bgColor}`}>
                    <div className="flex items-start gap-3">
                      <IconComponent className={`h-5 w-5 mt-0.5 flex-shrink-0 ${config.iconColor}`} />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${config.badgeColor}`}>
                            {config.label}
                          </span>
                        </div>
                        <p className={`text-sm font-medium ${config.textColor}`}>
                          {alerta.mensagem}
                        </p>
                        {alerta.sugestao && (
                          <div className="mt-2 flex items-start gap-2">
                            <Lightbulb className="h-4 w-4 mt-0.5 flex-shrink-0 text-gray-400 dark:text-gray-500" />
                            <p className="text-xs text-gray-600 dark:text-gray-400">
                              <strong>Sugestão:</strong> {alerta.sugestao}
                            </p>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Filtros */}
      <div className="mb-6 rounded-lg bg-white dark:bg-gray-800 p-4 shadow">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {/* Status */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Status
            </label>
            <select
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2"
              value={filtros.status || ''}
              onChange={(e) => handleFiltroChange('status', e.target.value as StatusDespesa || undefined)}
            >
              <option value="">Todos</option>
              <option value="PENDENTE">Pendente</option>
              <option value="PAGA">Paga</option>
              <option value="VENCIDA">Vencida</option>
              <option value="CANCELADA">Cancelada</option>
            </select>
          </div>

          {/* Categoria */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Categoria
            </label>
            <select
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2"
              value={filtros.categoria || ''}
              onChange={(e) => handleFiltroChange('categoria', e.target.value as CategoriaDespesa || undefined)}
            >
              <option value="">Todas</option>
              {Object.entries(CATEGORIAS_AGRUPADAS).map(([grupo, categorias]) => (
                <optgroup key={grupo} label={GRUPO_LABELS[grupo]}>
                  {categorias.map((cat) => (
                    <option key={cat.value} value={cat.value}>
                      {cat.label}
                    </option>
                  ))}
                </optgroup>
              ))}
            </select>
          </div>

          {/* Data Início */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Data Início
            </label>
            <input
              type="date"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2"
              value={filtros.dataInicio || ''}
              onChange={(e) => handleFiltroChange('dataInicio', e.target.value || undefined)}
            />
          </div>

          {/* Data Fim */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Data Fim
            </label>
            <input
              type="date"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2"
              value={filtros.dataFim || ''}
              onChange={(e) => handleFiltroChange('dataFim', e.target.value || undefined)}
            />
          </div>
        </div>

        {/* Limpar Filtros */}
        {Object.keys(filtros).length > 0 && (
          <div className="mt-4 flex justify-end">
            <button
              onClick={limparFiltros}
              className="flex items-center gap-2 rounded-lg border border-orange-300 dark:border-orange-700 bg-orange-50 dark:bg-orange-900/30 px-4 py-2 text-orange-700 dark:text-orange-400 hover:bg-orange-100 dark:hover:bg-orange-900/50"
            >
              <FilterX className="h-4 w-4" />
              Limpar Filtros
            </button>
          </div>
        )}
      </div>

      {/* Loading State */}
      {isLoading && (
        <div className="flex justify-center items-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 dark:border-blue-400 border-t-transparent"></div>
        </div>
      )}

      {/* Empty State */}
      {!isLoading && (!data || data.content.length === 0) && (
        <div className="rounded-lg bg-white dark:bg-gray-800 p-8 shadow text-center">
          <Receipt className="mx-auto h-12 w-12 text-gray-400" />
          <p className="mt-4 text-gray-500 dark:text-gray-400">Nenhuma despesa encontrada</p>
          <Link
            to="/financeiro/despesas/nova"
            className="mt-4 inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
          >
            <Plus className="h-4 w-4" />
            Cadastrar Despesa
          </Link>
        </div>
      )}

      {/* Mobile: Card Layout */}
      {!isLoading && data && data.content.length > 0 && (
        <div className="space-y-3 lg:hidden">
          {data.content.map((despesa) => (
            <div key={despesa.id} className="rounded-lg bg-white dark:bg-gray-800 p-4 shadow">
              {/* Header: Valor e Status */}
              <div className="flex items-start justify-between gap-2 mb-3">
                <div>
                  <div className="text-lg font-bold text-gray-900 dark:text-white">
                    {formatCurrency(despesa.valor)}
                  </div>
                  <div
                    className="text-sm font-medium"
                    style={{ color: despesa.categoriaCor }}
                  >
                    {despesa.categoriaDescricao}
                  </div>
                </div>
                <StatusBadge status={despesa.status} vencida={despesa.vencida} />
              </div>

              {/* Info */}
              <div className="text-sm text-gray-600 dark:text-gray-300 mb-3 pb-3 border-b border-gray-200 dark:border-gray-700">
                <p className="truncate">{despesa.descricao}</p>
                {despesa.fornecedor && (
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    {despesa.fornecedor}
                  </p>
                )}
              </div>

              <div className="grid grid-cols-2 gap-2 text-sm mb-3">
                <div>
                  <span className="text-gray-500 dark:text-gray-400">Venc: </span>
                  <span className="text-gray-900 dark:text-gray-100">{formatDate(despesa.dataVencimento)}</span>
                </div>
                {despesa.dataPagamento && (
                  <div>
                    <span className="text-gray-500 dark:text-gray-400">Pago: </span>
                    <span className="text-gray-900 dark:text-gray-100">{formatDate(despesa.dataPagamento)}</span>
                  </div>
                )}
              </div>

              {/* Ações */}
              <div className="flex items-center justify-end gap-2">
                {despesa.status === 'PENDENTE' && (
                  <>
                    <button
                      onClick={() => setPagarModalId(despesa.id)}
                      className="flex items-center gap-1 rounded-lg bg-green-600 px-3 py-1.5 text-sm text-white hover:bg-green-700"
                    >
                      <CheckCircle className="h-4 w-4" />
                      Pagar
                    </button>
                    <Link
                      to={`/financeiro/despesas/${despesa.id}/editar`}
                      className="rounded-lg border border-gray-300 dark:border-gray-600 p-1.5 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700"
                    >
                      <Edit className="h-4 w-4" />
                    </Link>
                    <button
                      onClick={() => handleCancelar(despesa.id)}
                      className="rounded-lg border border-red-300 dark:border-red-700 p-1.5 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30"
                    >
                      <XCircle className="h-4 w-4" />
                    </button>
                  </>
                )}
                {despesa.status === 'CANCELADA' && (
                  <button
                    onClick={() => handleExcluir(despesa.id)}
                    className="flex items-center gap-1 rounded-lg border border-red-300 dark:border-red-700 px-3 py-1.5 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30"
                  >
                    <Trash2 className="h-4 w-4" />
                    Excluir
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Desktop: Table Layout */}
      {!isLoading && data && data.content.length > 0 && (
        <div className="hidden lg:block rounded-lg bg-white dark:bg-gray-800 shadow">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Categoria
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Descrição
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Fornecedor
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Valor
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Vencimento
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Status
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Ações
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-800">
                {data.content.map((despesa) => (
                  <tr key={despesa.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                    <td className="px-6 py-4">
                      <span
                        className="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium"
                        style={{
                          backgroundColor: `${despesa.categoriaCor}20`,
                          color: despesa.categoriaCor,
                        }}
                      >
                        {despesa.categoriaDescricao}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900 dark:text-gray-100 max-w-xs truncate">
                      {despesa.descricao}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600 dark:text-gray-400">
                      {despesa.fornecedor || '-'}
                    </td>
                    <td className="px-6 py-4 text-sm font-semibold text-gray-900 dark:text-gray-100">
                      {formatCurrency(despesa.valor)}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900 dark:text-gray-100">
                      {formatDate(despesa.dataVencimento)}
                    </td>
                    <td className="px-6 py-4">
                      <StatusBadge status={despesa.status} vencida={despesa.vencida} />
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex justify-end gap-2">
                        {despesa.status === 'PENDENTE' && (
                          <>
                            <button
                              onClick={() => setPagarModalId(despesa.id)}
                              className="rounded p-1 text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/30"
                              title="Registrar Pagamento"
                            >
                              <CheckCircle className="h-4 w-4" />
                            </button>
                            <Link
                              to={`/financeiro/despesas/${despesa.id}/editar`}
                              className="rounded p-1 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/30"
                              title="Editar"
                            >
                              <Edit className="h-4 w-4" />
                            </Link>
                            <button
                              onClick={() => handleCancelar(despesa.id)}
                              className="rounded p-1 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30"
                              title="Cancelar"
                            >
                              <XCircle className="h-4 w-4" />
                            </button>
                          </>
                        )}
                        {despesa.status === 'CANCELADA' && (
                          <button
                            onClick={() => handleExcluir(despesa.id)}
                            className="rounded p-1 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30"
                            title="Excluir"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Paginação */}
      {data && data.totalPages > 1 && (
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between rounded-lg bg-white dark:bg-gray-800 px-4 sm:px-6 py-4 shadow">
          <div className="text-sm text-gray-700 dark:text-gray-300 text-center sm:text-left">
            Mostrando {page * ITEMS_PER_PAGE + 1} a{' '}
            {Math.min((page + 1) * ITEMS_PER_PAGE, data.totalElements)} de{' '}
            {data.totalElements} resultados
          </div>
          <div className="flex gap-2 justify-center sm:justify-end">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Anterior
            </button>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={page >= data.totalPages - 1}
              className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Próxima
            </button>
          </div>
        </div>
      )}

      {/* Modal de Pagamento */}
      <PagarModal
        isOpen={!!pagarModalId}
        onClose={() => setPagarModalId(null)}
        onConfirm={handlePagar}
        isLoading={pagarMutation.isPending}
      />
    </div>
  );
}
