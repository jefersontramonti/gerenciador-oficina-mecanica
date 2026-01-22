import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  RefreshCw,
  FileText,
  Calendar,
  CheckCircle,
  XCircle,
  Eye,
  CreditCard,
  Loader2,
} from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  useFaturas,
  useRegistrarPagamento,
  useCancelarFatura,
} from '../hooks/useAssinaturas';
import type {
  StatusFaturaAssinatura,
  FaturaAssinaturaDTO,
  RegistrarPagamentoDTO,
} from '../types/assinatura';
import { statusFaturaLabels, statusFaturaColors } from '../types/assinatura';
import { FeatureGate } from '@/shared/components/FeatureGate';

const pagamentoSchema = z.object({
  formaPagamento: z.string().optional(),
  observacao: z.string().optional(),
});

type PagamentoFormData = z.infer<typeof pagamentoSchema>;

export default function FaturasAssinaturaPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<StatusFaturaAssinatura | ''>('');
  const [pagamentoModalOpen, setPagamentoModalOpen] = useState(false);
  const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false);
  const [selectedFatura, setSelectedFatura] = useState<FaturaAssinaturaDTO | null>(null);

  const { data: faturasPage, isLoading, refetch } = useFaturas({
    status: statusFilter || undefined,
    page,
    size: 20,
  });

  const registrarPagamento = useRegistrarPagamento();
  const cancelarFatura = useCancelarFatura();

  const {
    register,
    handleSubmit,
    reset,
  } = useForm<PagamentoFormData>({
    resolver: zodResolver(pagamentoSchema),
  });

  const openPagamentoModal = (fatura: FaturaAssinaturaDTO) => {
    setSelectedFatura(fatura);
    reset();
    setPagamentoModalOpen(true);
  };

  const openCancelConfirm = (fatura: FaturaAssinaturaDTO) => {
    setSelectedFatura(fatura);
    setCancelConfirmOpen(true);
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

  const handleCancel = async () => {
    if (!selectedFatura) return;
    try {
      await cancelarFatura.mutateAsync({ faturaId: selectedFatura.id });
      setCancelConfirmOpen(false);
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

  // Cálculo de resumo
  const resumo = {
    pendentes: faturasPage?.content.filter((f) => f.status === 'PENDENTE').length || 0,
    vencidas: faturasPage?.content.filter((f) => f.status === 'VENCIDA').length || 0,
    pagas: faturasPage?.content.filter((f) => f.status === 'PAGA').length || 0,
    valorPendente: faturasPage?.content
      .filter((f) => f.status === 'PENDENTE' || f.status === 'VENCIDA')
      .reduce((sum, f) => sum + f.valor, 0) || 0,
  };

  return (
    <FeatureGate feature="COBRANCA_RECORRENTE" fallback={<div>Feature não disponível no seu plano</div>}>
      <div className="p-4 sm:p-6 space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
              Faturas de Assinatura
            </h1>
            <p className="text-gray-600 dark:text-gray-400 mt-1">
              Gerencie as faturas de cobranças recorrentes
            </p>
          </div>
          <button
            onClick={() => refetch()}
            className="inline-flex items-center px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
          >
            <RefreshCw className="w-4 h-4 mr-2" />
            Atualizar
          </button>
        </div>

        {/* Cards de Resumo */}
        {faturasPage && faturasPage.content && (
          <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
            <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
              <div className="flex items-center justify-between gap-2">
                <div className="min-w-0">
                  <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400">Pendentes</p>
                  <p className="mt-1 sm:mt-2 text-xl sm:text-2xl font-bold text-yellow-600 dark:text-yellow-400">
                    {resumo.pendentes}
                  </p>
                </div>
                <div className="rounded-full bg-yellow-100 dark:bg-yellow-900/30 p-2 sm:p-3 shrink-0">
                  <FileText className="h-5 w-5 sm:h-6 sm:w-6 text-yellow-600 dark:text-yellow-400" />
                </div>
              </div>
            </div>

            <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
              <div className="flex items-center justify-between gap-2">
                <div className="min-w-0">
                  <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400">Vencidas</p>
                  <p className="mt-1 sm:mt-2 text-xl sm:text-2xl font-bold text-red-600 dark:text-red-400">
                    {resumo.vencidas}
                  </p>
                </div>
                <div className="rounded-full bg-red-100 dark:bg-red-900/30 p-2 sm:p-3 shrink-0">
                  <Calendar className="h-5 w-5 sm:h-6 sm:w-6 text-red-600 dark:text-red-400" />
                </div>
              </div>
            </div>

            <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
              <div className="flex items-center justify-between gap-2">
                <div className="min-w-0">
                  <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400">Pagas</p>
                  <p className="mt-1 sm:mt-2 text-xl sm:text-2xl font-bold text-green-600 dark:text-green-400">
                    {resumo.pagas}
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
                  <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400 truncate">A Receber</p>
                  <p className="mt-1 sm:mt-2 text-lg sm:text-2xl font-bold text-blue-600 dark:text-blue-400 truncate">
                    {formatCurrency(resumo.valorPendente)}
                  </p>
                </div>
                <div className="rounded-full bg-blue-100 dark:bg-blue-900/30 p-2 sm:p-3 shrink-0">
                  <CreditCard className="h-5 w-5 sm:h-6 sm:w-6 text-blue-600 dark:text-blue-400" />
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Filtros */}
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 shadow">
          <div className="flex flex-wrap gap-4">
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value as StatusFaturaAssinatura | '');
                setPage(0);
              }}
              className="w-full sm:w-48 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            >
              <option value="">Todos os status</option>
              {Object.entries(statusFaturaLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Loading State */}
        {isLoading && (
          <div className="flex justify-center items-center py-12">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 dark:border-blue-400 border-t-transparent"></div>
          </div>
        )}

        {/* Empty State */}
        {!isLoading && (!faturasPage || faturasPage.content.length === 0) && (
          <div className="rounded-lg bg-white dark:bg-gray-800 p-8 shadow text-center">
            <FileText className="w-12 h-12 mx-auto text-gray-400 mb-4" />
            <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
              Nenhuma fatura encontrada
            </h3>
            <p className="text-gray-500 dark:text-gray-400">
              {statusFilter
                ? 'Tente ajustar os filtros'
                : 'As faturas serão geradas automaticamente para assinaturas ativas'}
            </p>
          </div>
        )}

        {/* Mobile: Card Layout */}
        {!isLoading && faturasPage && faturasPage.content.length > 0 && (
          <div className="space-y-3 lg:hidden">
            {faturasPage.content.map((fatura) => (
              <div
                key={fatura.id}
                className="rounded-lg bg-white dark:bg-gray-800 p-4 shadow"
              >
                {/* Header: Número e Status */}
                <div className="flex items-start justify-between gap-2 mb-3">
                  <div>
                    <div className="text-lg font-bold text-gray-900 dark:text-white">
                      {formatCurrency(fatura.valor)}
                    </div>
                    <div className="text-sm text-gray-500 dark:text-gray-400">
                      {fatura.numeroFatura}
                    </div>
                  </div>
                  <span
                    className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${
                      statusFaturaColors[fatura.status]
                    }`}
                  >
                    {statusFaturaLabels[fatura.status]}
                  </span>
                </div>

                {/* Info */}
                <div className="grid grid-cols-2 gap-2 text-sm mb-3 pb-3 border-b border-gray-200 dark:border-gray-700">
                  <div>
                    <span className="text-gray-500 dark:text-gray-400">Cliente: </span>
                    <span className="text-gray-900 dark:text-gray-100">{fatura.clienteNome}</span>
                  </div>
                  <div>
                    <span className="text-gray-500 dark:text-gray-400">Plano: </span>
                    <span className="text-gray-900 dark:text-gray-100">{fatura.planoNome}</span>
                  </div>
                  <div className="col-span-2">
                    <span className="text-gray-500 dark:text-gray-400">Venc: </span>
                    <span className={fatura.status === 'VENCIDA' ? 'text-red-600 dark:text-red-400' : 'text-gray-900 dark:text-gray-100'}>
                      {formatDate(fatura.dataVencimento)}
                    </span>
                  </div>
                </div>

                {/* Ações */}
                <div className="flex items-center justify-between gap-2">
                  <button
                    onClick={() => navigate(`/financeiro/assinaturas/${fatura.assinaturaId}`)}
                    className="flex items-center gap-1 text-sm text-blue-600 dark:text-blue-400 hover:text-blue-800"
                  >
                    Ver Assinatura
                    <Eye className="h-3 w-3" />
                  </button>
                  <div className="flex gap-2">
                    {(fatura.status === 'PENDENTE' || fatura.status === 'VENCIDA') && (
                      <>
                        <button
                          onClick={() => openPagamentoModal(fatura)}
                          disabled={registrarPagamento.isPending}
                          className="flex items-center gap-1 rounded-lg bg-green-600 px-3 py-1.5 text-sm text-white hover:bg-green-700 disabled:opacity-50"
                        >
                          <CheckCircle className="h-4 w-4" />
                          Pagar
                        </button>
                        <button
                          onClick={() => openCancelConfirm(fatura)}
                          disabled={cancelarFatura.isPending}
                          className="rounded-lg border border-red-600 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/30 disabled:opacity-50"
                        >
                          <XCircle className="h-4 w-4" />
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Desktop: Table Layout */}
        {!isLoading && faturasPage && faturasPage.content.length > 0 && (
          <div className="hidden lg:block rounded-lg bg-white dark:bg-gray-800 shadow">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Número
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Cliente
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Plano
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
                  {faturasPage.content.map((fatura) => (
                    <tr key={fatura.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-6 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">
                        {fatura.numeroFatura}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-900 dark:text-gray-100">
                        {fatura.clienteNome}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500 dark:text-gray-400">
                        {fatura.planoNome}
                      </td>
                      <td className="px-6 py-4 text-sm font-semibold text-gray-900 dark:text-gray-100">
                        {formatCurrency(fatura.valor)}
                      </td>
                      <td className="px-6 py-4 text-sm">
                        <span className={fatura.status === 'VENCIDA' ? 'text-red-600 dark:text-red-400' : 'text-gray-900 dark:text-gray-100'}>
                          {formatDate(fatura.dataVencimento)}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm">
                        <span
                          className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${
                            statusFaturaColors[fatura.status]
                          }`}
                        >
                          {statusFaturaLabels[fatura.status]}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right text-sm">
                        <div className="flex justify-end gap-2">
                          <button
                            onClick={() => navigate(`/financeiro/assinaturas/${fatura.assinaturaId}`)}
                            className="rounded p-1 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/30"
                            title="Ver Assinatura"
                          >
                            <Eye className="h-4 w-4" />
                          </button>
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
                                onClick={() => openCancelConfirm(fatura)}
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

        {/* Paginação */}
        {faturasPage && faturasPage.totalPages > 1 && (
          <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between rounded-lg bg-white dark:bg-gray-800 px-4 sm:px-6 py-4 shadow">
            <div className="text-sm text-gray-700 dark:text-gray-300 text-center sm:text-left">
              Página {page + 1} de {faturasPage.totalPages}
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
                disabled={page >= faturasPage.totalPages - 1}
                className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Próxima
              </button>
            </div>
          </div>
        )}

        {/* Modal de Pagamento */}
        {pagamentoModalOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-md p-6">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">
                Registrar Pagamento
              </h2>
              <form onSubmit={handleSubmit(onPagamentoSubmit)} className="space-y-4">
                <div className="bg-gray-50 dark:bg-gray-700 rounded-lg p-4 mb-4">
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    <strong>Fatura:</strong> {selectedFatura?.numeroFatura}
                  </p>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    <strong>Cliente:</strong> {selectedFatura?.clienteNome}
                  </p>
                  <p className="text-lg font-bold text-gray-900 dark:text-white mt-2">
                    {selectedFatura ? formatCurrency(selectedFatura.valor) : ''}
                  </p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Forma de Pagamento
                  </label>
                  <select
                    {...register('formaPagamento')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  >
                    <option value="">Selecione (opcional)</option>
                    <option value="PIX">PIX</option>
                    <option value="CARTAO_CREDITO">Cartão de Crédito</option>
                    <option value="CARTAO_DEBITO">Cartão de Débito</option>
                    <option value="BOLETO">Boleto</option>
                    <option value="DINHEIRO">Dinheiro</option>
                    <option value="TRANSFERENCIA">Transferência</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Observação
                  </label>
                  <input
                    {...register('observacao')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                    placeholder="Observação (opcional)"
                  />
                </div>

                <div className="flex justify-end gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setPagamentoModalOpen(false)}
                    className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    disabled={registrarPagamento.isPending}
                    className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 flex items-center gap-2"
                  >
                    {registrarPagamento.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                    Confirmar Pagamento
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Confirmação de Cancelamento */}
        {cancelConfirmOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                Cancelar Fatura
              </h2>
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                Deseja cancelar a fatura {selectedFatura?.numeroFatura} de {selectedFatura?.clienteNome}?
              </p>
              <div className="flex justify-end gap-3">
                <button
                  onClick={() => setCancelConfirmOpen(false)}
                  className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  Voltar
                </button>
                <button
                  onClick={handleCancel}
                  disabled={cancelarFatura.isPending}
                  className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 flex items-center gap-2"
                >
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
