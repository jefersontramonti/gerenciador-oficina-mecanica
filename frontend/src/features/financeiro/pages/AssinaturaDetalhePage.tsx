import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  User,
  Package,
  Calendar,
  CreditCard,
  FileText,
  Pause,
  Play,
  XCircle,
  CheckCircle,
  AlertTriangle,
  Loader2,
} from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  useAssinatura,
  useFaturasAssinatura,
  usePausarAssinatura,
  useReativarAssinatura,
  useCancelarAssinatura,
  useRegistrarPagamento,
  useCancelarFatura,
} from '../hooks/useAssinaturas';
import type {
  CancelarAssinaturaDTO,
  RegistrarPagamentoDTO,
  FaturaAssinaturaDTO,
} from '../types/assinatura';
import {
  statusAssinaturaLabels,
  statusAssinaturaColors,
  statusFaturaLabels,
  statusFaturaColors,
  periodicidadeLabels,
} from '../types/assinatura';
import { FeatureGate } from '@/shared/components/FeatureGate';

const cancelSchema = z.object({
  motivo: z.string().min(1, 'Informe o motivo'),
  cancelarFaturasPendentes: z.boolean().optional(),
});

const pagamentoSchema = z.object({
  formaPagamento: z.string().optional(),
  observacao: z.string().optional(),
});

type CancelFormData = z.infer<typeof cancelSchema>;
type PagamentoFormData = z.infer<typeof pagamentoSchema>;

export default function AssinaturaDetalhePage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [pauseConfirmOpen, setPauseConfirmOpen] = useState(false);
  const [reactivateConfirmOpen, setReactivateConfirmOpen] = useState(false);
  const [pagamentoModalOpen, setPagamentoModalOpen] = useState(false);
  const [cancelFaturaConfirmOpen, setCancelFaturaConfirmOpen] = useState(false);
  const [selectedFatura, setSelectedFatura] = useState<FaturaAssinaturaDTO | null>(null);

  const { data: assinatura, isLoading: loadingAssinatura } = useAssinatura(id!);
  const { data: faturas, isLoading: loadingFaturas } = useFaturasAssinatura(id!);

  const pausarAssinatura = usePausarAssinatura();
  const reativarAssinatura = useReativarAssinatura();
  const cancelarAssinatura = useCancelarAssinatura();
  const registrarPagamento = useRegistrarPagamento();
  const cancelarFatura = useCancelarFatura();

  const {
    register: registerCancel,
    handleSubmit: handleSubmitCancel,
    reset: resetCancel,
    formState: { errors: errorsCancel },
  } = useForm<CancelFormData>({
    resolver: zodResolver(cancelSchema),
    defaultValues: { cancelarFaturasPendentes: true },
  });

  const {
    register: registerPagamento,
    handleSubmit: handleSubmitPagamento,
    reset: resetPagamento,
  } = useForm<PagamentoFormData>({
    resolver: zodResolver(pagamentoSchema),
  });

  const openPagamentoModal = (fatura: FaturaAssinaturaDTO) => {
    setSelectedFatura(fatura);
    resetPagamento();
    setPagamentoModalOpen(true);
  };

  const openCancelFaturaConfirm = (fatura: FaturaAssinaturaDTO) => {
    setSelectedFatura(fatura);
    setCancelFaturaConfirmOpen(true);
  };

  const onCancelSubmit = async (data: CancelFormData) => {
    if (!assinatura) return;
    const dto: CancelarAssinaturaDTO = {
      motivo: data.motivo,
      cancelarFaturasPendentes: data.cancelarFaturasPendentes,
    };
    try {
      await cancelarAssinatura.mutateAsync({ id: assinatura.id, dto });
      setCancelModalOpen(false);
    } catch {
      // Error handled by mutation
    }
  };

  const onPagamentoSubmit = async (data: PagamentoFormData) => {
    if (!selectedFatura) return;
    const dto: RegistrarPagamentoDTO = {
      formaPagamento: data.formaPagamento,
      observacao: data.observacao,
    };
    try {
      await registrarPagamento.mutateAsync({ faturaId: selectedFatura.id, dto });
      setPagamentoModalOpen(false);
      setSelectedFatura(null);
    } catch {
      // Error handled by mutation
    }
  };

  const handlePause = async () => {
    if (!assinatura) return;
    try {
      await pausarAssinatura.mutateAsync(assinatura.id);
      setPauseConfirmOpen(false);
    } catch {
      // Error handled by mutation
    }
  };

  const handleReactivate = async () => {
    if (!assinatura) return;
    try {
      await reativarAssinatura.mutateAsync(assinatura.id);
      setReactivateConfirmOpen(false);
    } catch {
      // Error handled by mutation
    }
  };

  const handleCancelFatura = async () => {
    if (!selectedFatura) return;
    try {
      await cancelarFatura.mutateAsync({ faturaId: selectedFatura.id });
      setCancelFaturaConfirmOpen(false);
      setSelectedFatura(null);
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

  if (loadingAssinatura) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (!assinatura) {
    return (
      <div className="text-center py-12">
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
          Assinatura não encontrada
        </h2>
        <button
          onClick={() => navigate('/financeiro/assinaturas')}
          className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          Voltar
        </button>
      </div>
    );
  }

  return (
    <FeatureGate feature="COBRANCA_RECORRENTE" fallback={<div>Feature não disponível</div>}>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
              Assinatura - {assinatura.clienteNome}
            </h1>
            <p className="text-gray-600 dark:text-gray-400 mt-1">
              Plano {assinatura.planoNome}
            </p>
          </div>
          <div className="flex gap-2 flex-wrap">
            <button
              onClick={() => navigate('/financeiro/assinaturas')}
              className="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              <ArrowLeft className="w-4 h-4 mr-2" />
              Voltar
            </button>
            {assinatura.status === 'ATIVA' && (
              <button
                onClick={() => setPauseConfirmOpen(true)}
                className="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
              >
                <Pause className="w-4 h-4 mr-2" />
                Pausar
              </button>
            )}
            {(assinatura.status === 'PAUSADA' || assinatura.status === 'INADIMPLENTE') && (
              <button
                onClick={() => setReactivateConfirmOpen(true)}
                className="inline-flex items-center px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
              >
                <Play className="w-4 h-4 mr-2" />
                Reativar
              </button>
            )}
            {assinatura.status !== 'CANCELADA' && (
              <button
                onClick={() => {
                  resetCancel();
                  setCancelModalOpen(true);
                }}
                className="inline-flex items-center px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
              >
                <XCircle className="w-4 h-4 mr-2" />
                Cancelar
              </button>
            )}
          </div>
        </div>

        {/* Cards de Informação */}
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6">
            <div className="flex items-center gap-4">
              <div className="p-3 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                <User className="w-6 h-6 text-blue-600 dark:text-blue-400" />
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Cliente</p>
                <p className="font-semibold text-gray-900 dark:text-white">{assinatura.clienteNome}</p>
                <p className="text-sm text-gray-500">{assinatura.clienteCpfCnpj}</p>
              </div>
            </div>
          </div>

          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6">
            <div className="flex items-center gap-4">
              <div className="p-3 bg-purple-100 dark:bg-purple-900/30 rounded-lg">
                <Package className="w-6 h-6 text-purple-600 dark:text-purple-400" />
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Plano</p>
                <p className="font-semibold text-gray-900 dark:text-white">{assinatura.planoNome}</p>
                <p className="text-sm text-gray-500">{periodicidadeLabels[assinatura.periodicidade]}</p>
              </div>
            </div>
          </div>

          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6">
            <div className="flex items-center gap-4">
              <div className="p-3 bg-green-100 dark:bg-green-900/30 rounded-lg">
                <CreditCard className="w-6 h-6 text-green-600 dark:text-green-400" />
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Valor</p>
                <p className="font-semibold text-gray-900 dark:text-white">{formatCurrency(assinatura.valorAtual)}</p>
                <p className="text-sm text-gray-500">{assinatura.cobrancaAutomatica ? 'Automática' : 'Manual'}</p>
              </div>
            </div>
          </div>

          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6">
            <div className="flex items-center gap-4">
              <div
                className={`p-3 rounded-lg ${
                  assinatura.status === 'ATIVA'
                    ? 'bg-green-100 dark:bg-green-900/30'
                    : assinatura.status === 'PAUSADA'
                    ? 'bg-yellow-100 dark:bg-yellow-900/30'
                    : assinatura.status === 'INADIMPLENTE'
                    ? 'bg-red-100 dark:bg-red-900/30'
                    : 'bg-gray-100 dark:bg-gray-900/30'
                }`}
              >
                {assinatura.status === 'ATIVA' ? (
                  <CheckCircle className="w-6 h-6 text-green-600 dark:text-green-400" />
                ) : assinatura.status === 'PAUSADA' ? (
                  <Pause className="w-6 h-6 text-yellow-600 dark:text-yellow-400" />
                ) : assinatura.status === 'INADIMPLENTE' ? (
                  <AlertTriangle className="w-6 h-6 text-red-600 dark:text-red-400" />
                ) : (
                  <XCircle className="w-6 h-6 text-gray-600 dark:text-gray-400" />
                )}
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Status</p>
                <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${statusAssinaturaColors[assinatura.status]}`}>
                  {statusAssinaturaLabels[assinatura.status]}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Informações Detalhadas */}
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <Calendar className="w-5 h-5 text-gray-400" />
              Datas
            </h3>
            <dl className="space-y-3">
              <div className="flex justify-between">
                <dt className="text-gray-500 dark:text-gray-400">Início</dt>
                <dd className="text-gray-900 dark:text-white">{formatDate(assinatura.dataInicio)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500 dark:text-gray-400">Próximo Vencimento</dt>
                <dd className="text-gray-900 dark:text-white">{formatDate(assinatura.dataProximoVencimento)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500 dark:text-gray-400">Dia do Vencimento</dt>
                <dd className="text-gray-900 dark:text-white">Dia {assinatura.diaVencimento}</dd>
              </div>
            </dl>
          </div>

          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <FileText className="w-5 h-5 text-gray-400" />
              Resumo Financeiro
            </h3>
            <dl className="space-y-3">
              <div className="flex justify-between">
                <dt className="text-gray-500 dark:text-gray-400">Total de Faturas</dt>
                <dd className="text-gray-900 dark:text-white">{assinatura.totalFaturas}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500 dark:text-gray-400">Faturas Pagas</dt>
                <dd className="text-green-600 dark:text-green-400">{assinatura.faturasPagas}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500 dark:text-gray-400">Faturas Vencidas</dt>
                <dd className="text-red-600 dark:text-red-400">{assinatura.faturasVencidas}</dd>
              </div>
              <div className="flex justify-between pt-3 border-t border-gray-200 dark:border-gray-700">
                <dt className="text-gray-500 dark:text-gray-400">Total Pago</dt>
                <dd className="font-semibold text-gray-900 dark:text-white">{formatCurrency(assinatura.valorTotalPago)}</dd>
              </div>
            </dl>
          </div>
        </div>

        {/* Título da Seção de Faturas */}
        <div className="flex items-center gap-2 mb-4">
          <CreditCard className="w-5 h-5 text-gray-400" />
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Faturas</h3>
        </div>

        {/* Loading State */}
        {loadingFaturas && (
          <div className="flex justify-center items-center py-12">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 dark:border-blue-400 border-t-transparent"></div>
          </div>
        )}

        {/* Empty State */}
        {!loadingFaturas && (!faturas || faturas.length === 0) && (
          <div className="rounded-lg bg-white dark:bg-gray-800 p-8 shadow text-center">
            <CreditCard className="w-12 h-12 mx-auto text-gray-400 mb-4" />
            <p className="text-gray-500 dark:text-gray-400">Nenhuma fatura gerada ainda</p>
          </div>
        )}

        {/* Mobile: Card Layout */}
        {!loadingFaturas && faturas && faturas.length > 0 && (
          <div className="space-y-3 lg:hidden">
            {faturas.map((fatura) => (
              <div
                key={fatura.id}
                className="rounded-lg bg-white dark:bg-gray-800 p-4 shadow"
              >
                {/* Header: Valor e Status */}
                <div className="flex items-start justify-between gap-2 mb-3">
                  <div>
                    <div className="text-lg font-bold text-gray-900 dark:text-white">
                      {formatCurrency(fatura.valor)}
                    </div>
                    <div className="text-sm text-gray-500 dark:text-gray-400">
                      {fatura.numeroFatura}
                    </div>
                  </div>
                  <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${statusFaturaColors[fatura.status]}`}>
                    {statusFaturaLabels[fatura.status]}
                  </span>
                </div>

                {/* Info */}
                <div className="grid grid-cols-2 gap-2 text-sm mb-3 pb-3 border-b border-gray-200 dark:border-gray-700">
                  <div>
                    <span className="text-gray-500 dark:text-gray-400">Ref: </span>
                    <span className="text-gray-900 dark:text-gray-100">{formatDate(fatura.mesReferencia)}</span>
                  </div>
                  <div>
                    <span className="text-gray-500 dark:text-gray-400">Venc: </span>
                    <span className={fatura.status === 'VENCIDA' ? 'text-red-600 dark:text-red-400' : 'text-gray-900 dark:text-gray-100'}>
                      {formatDate(fatura.dataVencimento)}
                    </span>
                  </div>
                </div>

                {/* Ações */}
                <div className="flex items-center justify-end gap-2">
                  {(fatura.status === 'PENDENTE' || fatura.status === 'VENCIDA') && (
                    <>
                      <button
                        onClick={() => openPagamentoModal(fatura)}
                        className="flex items-center gap-1 rounded-lg bg-green-600 px-3 py-1.5 text-sm text-white hover:bg-green-700"
                      >
                        <CheckCircle className="h-4 w-4" />
                        Pagar
                      </button>
                      <button
                        onClick={() => openCancelFaturaConfirm(fatura)}
                        className="rounded-lg border border-red-600 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/30"
                      >
                        <XCircle className="h-4 w-4" />
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Desktop: Table Layout */}
        {!loadingFaturas && faturas && faturas.length > 0 && (
          <div className="hidden lg:block rounded-lg bg-white dark:bg-gray-800 shadow">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">Número</th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">Referência</th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">Valor</th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">Vencimento</th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">Status</th>
                    <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-800">
                  {faturas.map((fatura) => (
                    <tr key={fatura.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-6 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">{fatura.numeroFatura}</td>
                      <td className="px-6 py-4 text-sm text-gray-500 dark:text-gray-400">{formatDate(fatura.mesReferencia)}</td>
                      <td className="px-6 py-4 text-sm font-semibold text-gray-900 dark:text-gray-100">{formatCurrency(fatura.valor)}</td>
                      <td className="px-6 py-4 text-sm">
                        <span className={fatura.status === 'VENCIDA' ? 'text-red-600 dark:text-red-400' : 'text-gray-900 dark:text-gray-100'}>
                          {formatDate(fatura.dataVencimento)}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm">
                        <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${statusFaturaColors[fatura.status]}`}>
                          {statusFaturaLabels[fatura.status]}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right text-sm">
                        <div className="flex justify-end gap-2">
                          {(fatura.status === 'PENDENTE' || fatura.status === 'VENCIDA') && (
                            <>
                              <button
                                onClick={() => openPagamentoModal(fatura)}
                                className="rounded p-1 text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/30"
                                title="Registrar Pagamento"
                              >
                                <CheckCircle className="h-4 w-4" />
                              </button>
                              <button
                                onClick={() => openCancelFaturaConfirm(fatura)}
                                className="rounded p-1 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30"
                                title="Cancelar Fatura"
                              >
                                <XCircle className="h-4 w-4" />
                              </button>
                            </>
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

        {/* Modal de Cancelamento */}
        {cancelModalOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-md p-6">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Cancelar Assinatura</h2>
              <form onSubmit={handleSubmitCancel(onCancelSubmit)} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Motivo</label>
                  <input
                    {...registerCancel('motivo')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                    placeholder="Informe o motivo"
                  />
                  {errorsCancel.motivo && <p className="text-red-500 text-sm mt-1">{errorsCancel.motivo.message}</p>}
                </div>
                <div className="flex items-center gap-2">
                  <input type="checkbox" id="cancelarFaturas" {...registerCancel('cancelarFaturasPendentes')} className="w-4 h-4" />
                  <label htmlFor="cancelarFaturas" className="text-sm text-gray-700 dark:text-gray-300">Cancelar faturas pendentes</label>
                </div>
                <div className="flex justify-end gap-3 pt-4">
                  <button type="button" onClick={() => setCancelModalOpen(false)} className="px-4 py-2 border rounded-lg">Voltar</button>
                  <button type="submit" disabled={cancelarAssinatura.isPending} className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 flex items-center gap-2">
                    {cancelarAssinatura.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                    Confirmar
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Modal de Pagamento */}
        {pagamentoModalOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-md p-6">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Registrar Pagamento</h2>
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                Fatura: <strong>{selectedFatura?.numeroFatura}</strong> - {selectedFatura ? formatCurrency(selectedFatura.valor) : ''}
              </p>
              <form onSubmit={handleSubmitPagamento(onPagamentoSubmit)} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Forma de Pagamento</label>
                  <select {...registerPagamento('formaPagamento')} className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white">
                    <option value="">Selecione</option>
                    <option value="PIX">PIX</option>
                    <option value="CARTAO_CREDITO">Cartão de Crédito</option>
                    <option value="CARTAO_DEBITO">Cartão de Débito</option>
                    <option value="BOLETO">Boleto</option>
                    <option value="DINHEIRO">Dinheiro</option>
                    <option value="TRANSFERENCIA">Transferência</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Observação</label>
                  <input {...registerPagamento('observacao')} className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white" placeholder="Opcional" />
                </div>
                <div className="flex justify-end gap-3 pt-4">
                  <button type="button" onClick={() => setPagamentoModalOpen(false)} className="px-4 py-2 border rounded-lg">Cancelar</button>
                  <button type="submit" disabled={registrarPagamento.isPending} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 flex items-center gap-2">
                    {registrarPagamento.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                    Confirmar
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Confirmações */}
        {pauseConfirmOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">Pausar Assinatura</h2>
              <p className="text-gray-600 dark:text-gray-400 mb-4">Deseja pausar esta assinatura?</p>
              <div className="flex justify-end gap-3">
                <button onClick={() => setPauseConfirmOpen(false)} className="px-4 py-2 border rounded-lg">Cancelar</button>
                <button onClick={handlePause} disabled={pausarAssinatura.isPending} className="px-4 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700 disabled:opacity-50 flex items-center gap-2">
                  {pausarAssinatura.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  Pausar
                </button>
              </div>
            </div>
          </div>
        )}

        {reactivateConfirmOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">Reativar Assinatura</h2>
              <p className="text-gray-600 dark:text-gray-400 mb-4">Deseja reativar esta assinatura?</p>
              <div className="flex justify-end gap-3">
                <button onClick={() => setReactivateConfirmOpen(false)} className="px-4 py-2 border rounded-lg">Cancelar</button>
                <button onClick={handleReactivate} disabled={reativarAssinatura.isPending} className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 flex items-center gap-2">
                  {reativarAssinatura.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  Reativar
                </button>
              </div>
            </div>
          </div>
        )}

        {cancelFaturaConfirmOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">Cancelar Fatura</h2>
              <p className="text-gray-600 dark:text-gray-400 mb-4">Deseja cancelar a fatura {selectedFatura?.numeroFatura}?</p>
              <div className="flex justify-end gap-3">
                <button onClick={() => setCancelFaturaConfirmOpen(false)} className="px-4 py-2 border rounded-lg">Voltar</button>
                <button onClick={handleCancelFatura} disabled={cancelarFatura.isPending} className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 flex items-center gap-2">
                  {cancelarFatura.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  Cancelar Fatura
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </FeatureGate>
  );
}
