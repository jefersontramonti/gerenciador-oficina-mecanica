import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Plus,
  Search,
  RefreshCw,
  Users,
  Pause,
  Play,
  XCircle,
  Eye,
  Calendar,
  CreditCard,
  Loader2,
  AlertTriangle,
  AlertCircle,
  Info,
  ChevronDown,
  ChevronUp,
  Lightbulb,
} from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  useAssinaturas,
  usePlanosAtivos,
  useCriarAssinatura,
  usePausarAssinatura,
  useReativarAssinatura,
  useCancelarAssinatura,
} from '../hooks/useAssinaturas';
import { useClientes } from '@/features/clientes/hooks/useClientes';
import type {
  StatusAssinatura,
  CreateAssinaturaDTO,
  CancelarAssinaturaDTO,
  AssinaturaDTO,
} from '../types/assinatura';
import {
  statusAssinaturaLabels,
  statusAssinaturaColors,
  periodicidadeLabels,
} from '../types/assinatura';
import { FeatureGate } from '@/shared/components/FeatureGate';

const createSchema = z.object({
  clienteId: z.string().min(1, 'Selecione um cliente'),
  planoId: z.string().min(1, 'Selecione um plano'),
  diaVencimento: z.number().min(1).max(28).optional(),
  cobrancaAutomatica: z.boolean().optional(),
});

const cancelSchema = z.object({
  motivo: z.string().min(1, 'Informe o motivo do cancelamento'),
  cancelarFaturasPendentes: z.boolean().optional(),
});

type CreateFormData = z.infer<typeof createSchema>;
type CancelFormData = z.infer<typeof cancelSchema>;

// Tipos de alertas de Assinaturas
type NivelAlertaAssinatura = 'CRITICAL' | 'WARNING' | 'INFO';

interface AlertaAssinatura {
  nivel: NivelAlertaAssinatura;
  mensagem: string;
  sugestao?: string;
}

// Função para gerar alertas baseados nas assinaturas
function gerarAlertasAssinaturas(assinaturas: AssinaturaDTO[]): AlertaAssinatura[] {
  const alertas: AlertaAssinatura[] = [];

  if (!assinaturas || assinaturas.length === 0) return alertas;

  // Contar inadimplentes
  const inadimplentes = assinaturas.filter((a) => a.status === 'INADIMPLENTE');
  if (inadimplentes.length > 0) {
    alertas.push({
      nivel: 'CRITICAL',
      mensagem: `${inadimplentes.length} assinatura(s) inadimplente(s) com pagamentos em atraso`,
      sugestao: 'Entre em contato com os clientes para regularizar os pagamentos. Considere enviar lembretes automáticos.',
    });
  }

  // Contar total de faturas vencidas
  const totalFaturasVencidas = assinaturas.reduce((acc, a) => acc + (a.faturasVencidas || 0), 0);
  if (totalFaturasVencidas > 0) {
    alertas.push({
      nivel: 'WARNING',
      mensagem: `${totalFaturasVencidas} fatura(s) vencida(s) aguardando pagamento`,
      sugestao: 'Verifique as faturas vencidas e entre em contato com os clientes para regularização.',
    });
  }

  // Contar assinaturas pausadas
  const pausadas = assinaturas.filter((a) => a.status === 'PAUSADA');
  if (pausadas.length > 0) {
    alertas.push({
      nivel: 'INFO',
      mensagem: `${pausadas.length} assinatura(s) pausada(s) - potencial de reativação`,
      sugestao: 'Considere oferecer condições especiais para reativar essas assinaturas.',
    });
  }

  // Alerta positivo: muitas assinaturas ativas
  const ativas = assinaturas.filter((a) => a.status === 'ATIVA');
  if (ativas.length >= 10) {
    alertas.push({
      nivel: 'INFO',
      mensagem: `${ativas.length} assinaturas ativas - bom desempenho de recorrência!`,
    });
  }

  return alertas;
}

// Configuração visual dos alertas
function getAlertaConfigAssinatura(nivel: NivelAlertaAssinatura) {
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

export default function AssinaturasPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<StatusAssinatura | ''>('');
  const [planoFilter, setPlanoFilter] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [pauseConfirmOpen, setPauseConfirmOpen] = useState(false);
  const [reactivateConfirmOpen, setReactivateConfirmOpen] = useState(false);
  const [selectedAssinatura, setSelectedAssinatura] = useState<AssinaturaDTO | null>(null);
  const [alertasExpandidos, setAlertasExpandidos] = useState(true);

  const { data: assinaturasPage, isLoading, isFetching, refetch } = useAssinaturas({
    status: statusFilter || undefined,
    planoId: planoFilter || undefined,
    busca: searchTerm || undefined,
    page,
    size: 10,
  });

  const { data: planos } = usePlanosAtivos();
  const { data: clientesPage } = useClientes({ page: 0, size: 100, ativo: true });

  const criarAssinatura = useCriarAssinatura();
  const pausarAssinatura = usePausarAssinatura();
  const reativarAssinatura = useReativarAssinatura();
  const cancelarAssinatura = useCancelarAssinatura();

  const {
    register: registerCreate,
    handleSubmit: handleSubmitCreate,
    reset: resetCreate,
    formState: { errors: errorsCreate },
  } = useForm<CreateFormData>({
    resolver: zodResolver(createSchema),
    defaultValues: {
      diaVencimento: 5,
      cobrancaAutomatica: false,
    },
  });

  const {
    register: registerCancel,
    handleSubmit: handleSubmitCancel,
    reset: resetCancel,
    formState: { errors: errorsCancel },
  } = useForm<CancelFormData>({
    resolver: zodResolver(cancelSchema),
    defaultValues: {
      cancelarFaturasPendentes: true,
    },
  });

  const clientes = useMemo(() => clientesPage?.content || [], [clientesPage]);

  // Gerar alertas inteligentes baseados nas assinaturas
  const alertas = useMemo(() => {
    if (!assinaturasPage?.content) return [];
    return gerarAlertasAssinaturas(assinaturasPage.content);
  }, [assinaturasPage?.content]);

  const alertasCount = useMemo(() => ({
    critical: alertas.filter((a) => a.nivel === 'CRITICAL').length,
    warning: alertas.filter((a) => a.nivel === 'WARNING').length,
    info: alertas.filter((a) => a.nivel === 'INFO').length,
    total: alertas.length,
  }), [alertas]);

  const openCreateModal = () => {
    resetCreate();
    setCreateModalOpen(true);
  };

  const openCancelModal = (assinatura: AssinaturaDTO) => {
    setSelectedAssinatura(assinatura);
    resetCancel();
    setCancelModalOpen(true);
  };

  const openPauseConfirm = (assinatura: AssinaturaDTO) => {
    setSelectedAssinatura(assinatura);
    setPauseConfirmOpen(true);
  };

  const openReactivateConfirm = (assinatura: AssinaturaDTO) => {
    setSelectedAssinatura(assinatura);
    setReactivateConfirmOpen(true);
  };

  const onCreateSubmit = async (data: CreateFormData) => {
    const dto: CreateAssinaturaDTO = {
      clienteId: data.clienteId,
      planoId: data.planoId,
      diaVencimento: data.diaVencimento,
      cobrancaAutomatica: data.cobrancaAutomatica,
    };

    try {
      await criarAssinatura.mutateAsync(dto);
      setCreateModalOpen(false);
    } catch {
      // Error handled by mutation
    }
  };

  const onCancelSubmit = async (data: CancelFormData) => {
    if (!selectedAssinatura) return;

    const dto: CancelarAssinaturaDTO = {
      motivo: data.motivo,
      cancelarFaturasPendentes: data.cancelarFaturasPendentes,
    };

    try {
      await cancelarAssinatura.mutateAsync({ id: selectedAssinatura.id, dto });
      setCancelModalOpen(false);
      setSelectedAssinatura(null);
    } catch {
      // Error handled by mutation
    }
  };

  const handlePause = async () => {
    if (!selectedAssinatura) return;
    try {
      await pausarAssinatura.mutateAsync(selectedAssinatura.id);
      setPauseConfirmOpen(false);
      setSelectedAssinatura(null);
    } catch {
      // Error handled by mutation
    }
  };

  const handleReactivate = async () => {
    if (!selectedAssinatura) return;
    try {
      await reativarAssinatura.mutateAsync(selectedAssinatura.id);
      setReactivateConfirmOpen(false);
      setSelectedAssinatura(null);
    } catch {
      // Error handled by mutation
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('pt-BR');
  };

  return (
    <FeatureGate feature="COBRANCA_RECORRENTE" fallback={<div>Feature não disponível no seu plano</div>}>
      <div className="p-4 sm:p-6 space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
              Assinaturas
            </h1>
            <p className="text-gray-600 dark:text-gray-400 mt-1">
              Gerencie as assinaturas de clientes
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => refetch()}
              disabled={isFetching}
              className="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 mr-2 ${isFetching ? 'animate-spin' : ''}`} />
              Atualizar
            </button>
            <button
              onClick={openCreateModal}
              className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              <Plus className="w-4 h-4 mr-2" />
              Nova Assinatura
            </button>
          </div>
        </div>

        {/* Filtros */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-4">
          <div className="grid gap-4 md:grid-cols-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="Buscar por cliente..."
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value);
                  setPage(0);
                }}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              />
            </div>

            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value as StatusAssinatura | '');
                setPage(0);
              }}
              className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            >
              <option value="">Todos os status</option>
              {Object.entries(statusAssinaturaLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>

            <select
              value={planoFilter}
              onChange={(e) => {
                setPlanoFilter(e.target.value);
                setPage(0);
              }}
              className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            >
              <option value="">Todos os planos</option>
              {planos?.map((plano) => (
                <option key={plano.id} value={plano.id}>
                  {plano.nome}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Alertas Inteligentes */}
        {alertas.length > 0 && (
          <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden shadow">
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
                  const config = getAlertaConfigAssinatura(alerta.nivel);
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

        {/* Mobile Cards */}
        <div className="lg:hidden space-y-3">
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
            </div>
          ) : assinaturasPage?.content.length === 0 ? (
            <div className="rounded-lg bg-white dark:bg-gray-800 p-8 text-center shadow">
              <Users className="w-12 h-12 mx-auto text-gray-400 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
                Nenhuma assinatura encontrada
              </h3>
              <p className="text-gray-500 dark:text-gray-400 mb-4">
                {searchTerm || statusFilter || planoFilter
                  ? 'Tente ajustar os filtros'
                  : 'Crie a primeira assinatura'}
              </p>
              {!searchTerm && !statusFilter && !planoFilter && (
                <button
                  onClick={openCreateModal}
                  className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                >
                  <Plus className="w-4 h-4 mr-2" />
                  Nova Assinatura
                </button>
              )}
            </div>
          ) : (
            <>
              {assinaturasPage?.content.map((assinatura) => (
                <div
                  key={assinatura.id}
                  className="rounded-lg bg-white dark:bg-gray-800 p-4 shadow border border-gray-200 dark:border-gray-700"
                >
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-3 flex-1 min-w-0">
                      <div className="w-10 h-10 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center flex-shrink-0">
                        <Users className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                      </div>
                      <div className="min-w-0">
                        <h3 className="font-medium text-gray-900 dark:text-white truncate">
                          {assinatura.clienteNome}
                        </h3>
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                          {assinatura.planoNome}
                        </p>
                      </div>
                    </div>
                    <span
                      className={`inline-flex px-2 py-1 text-xs font-medium rounded-full flex-shrink-0 ${
                        statusAssinaturaColors[assinatura.status]
                      }`}
                    >
                      {statusAssinaturaLabels[assinatura.status]}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-sm mb-3">
                    <div>
                      <span className="text-gray-500 dark:text-gray-400">Valor:</span>
                      <span className="ml-1 font-medium text-gray-900 dark:text-white">
                        {formatCurrency(assinatura.valorAtual)}
                      </span>
                    </div>
                    <div>
                      <span className="text-gray-500 dark:text-gray-400">Periodicidade:</span>
                      <span className="ml-1 text-gray-900 dark:text-white">
                        {periodicidadeLabels[assinatura.periodicidade]}
                      </span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Calendar className="w-3 h-3 text-gray-400" />
                      <span className="text-gray-500 dark:text-gray-400">Venc.:</span>
                      <span className="text-gray-900 dark:text-white">
                        {formatDate(assinatura.dataProximoVencimento)}
                      </span>
                    </div>
                    <div className="flex items-center gap-1">
                      <CreditCard className="w-3 h-3 text-gray-400" />
                      <span className="text-gray-900 dark:text-white">
                        {assinatura.faturasPagas}/{assinatura.totalFaturas}
                      </span>
                      {assinatura.faturasVencidas > 0 && (
                        <span className="text-xs text-red-600 dark:text-red-400">
                          ({assinatura.faturasVencidas} venc.)
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="flex gap-2 pt-3 border-t border-gray-200 dark:border-gray-700">
                    <button
                      onClick={() => navigate(`/financeiro/assinaturas/${assinatura.id}`)}
                      className="flex-1 flex items-center justify-center gap-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 px-3 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                    >
                      <Eye className="w-4 h-4" />
                      Ver
                    </button>
                    {assinatura.status === 'ATIVA' && (
                      <button
                        onClick={() => openPauseConfirm(assinatura)}
                        className="flex items-center justify-center gap-1 rounded-lg border border-yellow-300 dark:border-yellow-700 bg-yellow-50 dark:bg-yellow-900/20 px-3 py-2 text-sm font-medium text-yellow-700 dark:text-yellow-400 hover:bg-yellow-100 dark:hover:bg-yellow-900/30"
                      >
                        <Pause className="w-4 h-4" />
                      </button>
                    )}
                    {(assinatura.status === 'PAUSADA' || assinatura.status === 'INADIMPLENTE') && (
                      <button
                        onClick={() => openReactivateConfirm(assinatura)}
                        className="flex items-center justify-center gap-1 rounded-lg border border-green-300 dark:border-green-700 bg-green-50 dark:bg-green-900/20 px-3 py-2 text-sm font-medium text-green-700 dark:text-green-400 hover:bg-green-100 dark:hover:bg-green-900/30"
                      >
                        <Play className="w-4 h-4" />
                      </button>
                    )}
                    {assinatura.status !== 'CANCELADA' && (
                      <button
                        onClick={() => openCancelModal(assinatura)}
                        className="flex items-center justify-center gap-1 rounded-lg border border-red-300 dark:border-red-700 bg-red-50 dark:bg-red-900/20 px-3 py-2 text-sm font-medium text-red-700 dark:text-red-400 hover:bg-red-100 dark:hover:bg-red-900/30"
                      >
                        <XCircle className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>
              ))}
              {/* Mobile Pagination */}
              {assinaturasPage && assinaturasPage.totalPages > 1 && (
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between bg-white dark:bg-gray-800 rounded-lg p-4 shadow">
                  <div className="text-sm text-gray-700 dark:text-gray-300 text-center sm:text-left">
                    Página {page + 1} de {assinaturasPage.totalPages}
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={page === 0}
                      className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      Anterior
                    </button>
                    <button
                      onClick={() => setPage((p) => Math.min(assinaturasPage.totalPages - 1, p + 1))}
                      disabled={page >= assinaturasPage.totalPages - 1}
                      className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      Próxima
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Desktop Table */}
        <div className="hidden lg:block bg-white dark:bg-gray-800 rounded-lg shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Cliente
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Plano
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Valor
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Próximo Vencimento
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Faturas
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Ações
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {isLoading ? (
                  <tr>
                    <td colSpan={7} className="px-6 py-12 text-center">
                      <Loader2 className="w-8 h-8 animate-spin mx-auto text-blue-600" />
                    </td>
                  </tr>
                ) : assinaturasPage?.content.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-6 py-12 text-center">
                      <Users className="w-12 h-12 mx-auto text-gray-400 mb-4" />
                      <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
                        Nenhuma assinatura encontrada
                      </h3>
                      <p className="text-gray-500 dark:text-gray-400 mb-4">
                        {searchTerm || statusFilter || planoFilter
                          ? 'Tente ajustar os filtros'
                          : 'Crie a primeira assinatura'}
                      </p>
                      {!searchTerm && !statusFilter && !planoFilter && (
                        <button
                          onClick={openCreateModal}
                          className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                        >
                          <Plus className="w-4 h-4 mr-2" />
                          Nova Assinatura
                        </button>
                      )}
                    </td>
                  </tr>
                ) : (
                  assinaturasPage?.content.map((assinatura) => (
                    <tr key={assinatura.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
                            <Users className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                          </div>
                          <div>
                            <p className="font-medium text-gray-900 dark:text-white">
                              {assinatura.clienteNome}
                            </p>
                            <p className="text-sm text-gray-500 dark:text-gray-400">
                              {assinatura.clienteCpfCnpj}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div>
                          <p className="text-gray-900 dark:text-white">{assinatura.planoNome}</p>
                          <p className="text-sm text-gray-500 dark:text-gray-400">
                            {periodicidadeLabels[assinatura.periodicidade]}
                          </p>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <span className="font-medium text-gray-900 dark:text-white">
                          {formatCurrency(assinatura.valorAtual)}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <span
                          className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${
                            statusAssinaturaColors[assinatura.status]
                          }`}
                        >
                          {statusAssinaturaLabels[assinatura.status]}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <Calendar className="w-4 h-4 text-gray-400" />
                          <span className="text-gray-900 dark:text-white">
                            {formatDate(assinatura.dataProximoVencimento)}
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <CreditCard className="w-4 h-4 text-gray-400" />
                          <span className="text-gray-900 dark:text-white">
                            {assinatura.faturasPagas}/{assinatura.totalFaturas}
                          </span>
                          {assinatura.faturasVencidas > 0 && (
                            <span className="text-xs text-red-600 dark:text-red-400">
                              ({assinatura.faturasVencidas} vencidas)
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex justify-end gap-1">
                          <button
                            onClick={() => navigate(`/financeiro/assinaturas/${assinatura.id}`)}
                            className="p-1.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded"
                            title="Ver detalhes"
                          >
                            <Eye className="w-4 h-4" />
                          </button>

                          {assinatura.status === 'ATIVA' && (
                            <button
                              onClick={() => openPauseConfirm(assinatura)}
                              className="p-1.5 text-gray-500 hover:text-yellow-600 hover:bg-yellow-50 dark:hover:bg-yellow-900/20 rounded"
                              title="Pausar"
                            >
                              <Pause className="w-4 h-4" />
                            </button>
                          )}

                          {(assinatura.status === 'PAUSADA' || assinatura.status === 'INADIMPLENTE') && (
                            <button
                              onClick={() => openReactivateConfirm(assinatura)}
                              className="p-1.5 text-gray-500 hover:text-green-600 hover:bg-green-50 dark:hover:bg-green-900/20 rounded"
                              title="Reativar"
                            >
                              <Play className="w-4 h-4" />
                            </button>
                          )}

                          {assinatura.status !== 'CANCELADA' && (
                            <button
                              onClick={() => openCancelModal(assinatura)}
                              className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded"
                              title="Cancelar"
                            >
                              <XCircle className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {assinaturasPage && assinaturasPage.totalPages > 1 && (
            <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-between items-center">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded text-gray-700 dark:text-gray-300 disabled:opacity-50"
              >
                Anterior
              </button>
              <span className="text-sm text-gray-600 dark:text-gray-400">
                Página {page + 1} de {assinaturasPage.totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(assinaturasPage.totalPages - 1, p + 1))}
                disabled={page >= assinaturasPage.totalPages - 1}
                className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded text-gray-700 dark:text-gray-300 disabled:opacity-50"
              >
                Próxima
              </button>
            </div>
          )}
        </div>

        {/* Modal de Criação */}
        {createModalOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-md p-6">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">
                Nova Assinatura
              </h2>
              <form onSubmit={handleSubmitCreate(onCreateSubmit)} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Cliente
                  </label>
                  <select
                    {...registerCreate('clienteId')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  >
                    <option value="">Selecione um cliente</option>
                    {clientes.map((cliente) => (
                      <option key={cliente.id} value={cliente.id}>
                        {cliente.nome} - {cliente.cpfCnpj}
                      </option>
                    ))}
                  </select>
                  {errorsCreate.clienteId && (
                    <p className="text-red-500 text-sm mt-1">{errorsCreate.clienteId.message}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Plano
                  </label>
                  <select
                    {...registerCreate('planoId')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  >
                    <option value="">Selecione um plano</option>
                    {planos?.map((plano) => (
                      <option key={plano.id} value={plano.id}>
                        {plano.nome} - {formatCurrency(plano.valor)}/{periodicidadeLabels[plano.periodicidade].toLowerCase()}
                      </option>
                    ))}
                  </select>
                  {errorsCreate.planoId && (
                    <p className="text-red-500 text-sm mt-1">{errorsCreate.planoId.message}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Dia do Vencimento
                  </label>
                  <input
                    type="number"
                    min={1}
                    max={28}
                    {...registerCreate('diaVencimento', { valueAsNumber: true })}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  />
                </div>

                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    id="cobrancaAutomatica"
                    {...registerCreate('cobrancaAutomatica')}
                    className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                  />
                  <label htmlFor="cobrancaAutomatica" className="text-sm text-gray-700 dark:text-gray-300">
                    Habilitar cobrança automática
                  </label>
                </div>

                <div className="flex justify-end gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setCreateModalOpen(false)}
                    className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    disabled={criarAssinatura.isPending}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 flex items-center gap-2"
                  >
                    {criarAssinatura.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                    Criar Assinatura
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Modal de Cancelamento */}
        {cancelModalOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-md p-6">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">
                Cancelar Assinatura
              </h2>
              <form onSubmit={handleSubmitCancel(onCancelSubmit)} className="space-y-4">
                <p className="text-gray-600 dark:text-gray-400">
                  Você está cancelando a assinatura de <strong>{selectedAssinatura?.clienteNome}</strong>.
                </p>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Motivo do Cancelamento
                  </label>
                  <input
                    {...registerCancel('motivo')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                    placeholder="Informe o motivo"
                  />
                  {errorsCancel.motivo && (
                    <p className="text-red-500 text-sm mt-1">{errorsCancel.motivo.message}</p>
                  )}
                </div>

                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    id="cancelarFaturas"
                    {...registerCancel('cancelarFaturasPendentes')}
                    className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                  />
                  <label htmlFor="cancelarFaturas" className="text-sm text-gray-700 dark:text-gray-300">
                    Cancelar faturas pendentes
                  </label>
                </div>

                <div className="flex justify-end gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setCancelModalOpen(false)}
                    className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
                  >
                    Voltar
                  </button>
                  <button
                    type="submit"
                    disabled={cancelarAssinatura.isPending}
                    className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 flex items-center gap-2"
                  >
                    {cancelarAssinatura.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                    Confirmar Cancelamento
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Confirmação de Pausa */}
        {pauseConfirmOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                Pausar Assinatura
              </h2>
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                Deseja pausar a assinatura de {selectedAssinatura?.clienteNome}? A cobrança será suspensa até que seja reativada.
              </p>
              <div className="flex justify-end gap-3">
                <button
                  onClick={() => setPauseConfirmOpen(false)}
                  className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  Cancelar
                </button>
                <button
                  onClick={handlePause}
                  disabled={pausarAssinatura.isPending}
                  className="px-4 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700 disabled:opacity-50 flex items-center gap-2"
                >
                  {pausarAssinatura.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  Pausar
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Confirmação de Reativação */}
        {reactivateConfirmOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                Reativar Assinatura
              </h2>
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                Deseja reativar a assinatura de {selectedAssinatura?.clienteNome}? A cobrança será retomada normalmente.
              </p>
              <div className="flex justify-end gap-3">
                <button
                  onClick={() => setReactivateConfirmOpen(false)}
                  className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  Cancelar
                </button>
                <button
                  onClick={handleReactivate}
                  disabled={reativarAssinatura.isPending}
                  className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 flex items-center gap-2"
                >
                  {reativarAssinatura.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  Reativar
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </FeatureGate>
  );
}
