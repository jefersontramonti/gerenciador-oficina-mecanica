import { useState } from 'react';
import { Link } from 'react-router-dom';
import { FileText, Eye, DollarSign, X, RefreshCw, Plus } from 'lucide-react';
import { useFaturas, useFaturasSummary, useCancelarFatura, useGerarFaturasMensais } from '../hooks/useSaas';
import type { StatusFatura, FaturaFilters, FaturaResumo } from '../types';
import { statusFaturaLabels } from '../types';
import { RegistrarPagamentoModal } from '../components/RegistrarPagamentoModal';

const statusColors: Record<StatusFatura, string> = {
  PENDENTE: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
  PAGO: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
  VENCIDO: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
  CANCELADO: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
};

export function FaturasListPage() {
  const [filters, setFilters] = useState<FaturaFilters>({
    page: 0,
    size: 20,
  });
  const [selectedFatura, setSelectedFatura] = useState<FaturaResumo | null>(null);
  const [showPaymentModal, setShowPaymentModal] = useState(false);

  const { data: faturas, isLoading } = useFaturas(filters);
  const { data: summary } = useFaturasSummary();
  const cancelarFatura = useCancelarFatura();
  const gerarMensais = useGerarFaturasMensais();

  const handleStatusFilter = (status: StatusFatura | undefined) => {
    setFilters({ ...filters, status, page: 0 });
  };

  const handleRegistrarPagamento = (fatura: FaturaResumo) => {
    setSelectedFatura(fatura);
    setShowPaymentModal(true);
  };

  const handleCancelar = async (fatura: FaturaResumo) => {
    if (confirm(`Deseja cancelar a fatura ${fatura.numero}?`)) {
      await cancelarFatura.mutateAsync({ id: fatura.id, motivo: 'Cancelado pelo administrador' });
    }
  };

  const handleGerarMensais = async () => {
    if (confirm('Deseja gerar faturas mensais para todas as oficinas ativas?')) {
      const result = await gerarMensais.mutateAsync();
      alert(`${result.count} faturas geradas com sucesso!`);
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  };

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('pt-BR');
  };

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Faturas</h1>
          <p className="text-gray-600 dark:text-gray-400">Gerenciamento de faturas e cobranças</p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={handleGerarMensais}
            disabled={gerarMensais.isPending}
            className="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-gray-700 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700"
          >
            <RefreshCw className={`h-4 w-4 ${gerarMensais.isPending ? 'animate-spin' : ''}`} />
            Gerar Mensais
          </button>
          <Link
            to="/admin/faturas/nova"
            className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 dark:bg-blue-500 dark:hover:bg-blue-600"
          >
            <Plus className="h-4 w-4" />
            Nova Fatura
          </Link>
        </div>
      </div>

      {/* Summary Cards */}
      {summary && (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
          <div className="rounded-lg bg-yellow-50 p-4 dark:bg-yellow-900/20">
            <p className="text-sm text-yellow-700 dark:text-yellow-400">Pendentes</p>
            <p className="text-2xl font-bold text-yellow-900 dark:text-yellow-300">{summary.totalPendentes}</p>
            <p className="text-sm text-yellow-600 dark:text-yellow-500">{formatCurrency(summary.valorPendente)}</p>
          </div>
          <div className="rounded-lg bg-red-50 p-4 dark:bg-red-900/20">
            <p className="text-sm text-red-700 dark:text-red-400">Vencidas</p>
            <p className="text-2xl font-bold text-red-900 dark:text-red-300">{summary.totalVencidas}</p>
            <p className="text-sm text-red-600 dark:text-red-500">{formatCurrency(summary.valorVencido)}</p>
          </div>
          <div className="rounded-lg bg-green-50 p-4 dark:bg-green-900/20">
            <p className="text-sm text-green-700 dark:text-green-400">Pagas</p>
            <p className="text-2xl font-bold text-green-900 dark:text-green-300">{summary.totalPagas}</p>
            <p className="text-sm text-green-600 dark:text-green-500">{formatCurrency(summary.valorRecebidoMes)} este mês</p>
          </div>
          <div className="rounded-lg bg-gray-50 p-4 dark:bg-gray-800">
            <p className="text-sm text-gray-700 dark:text-gray-400">Oficinas Inadimplentes</p>
            <p className="text-2xl font-bold text-gray-900 dark:text-white">{summary.oficinasInadimplentes}</p>
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-4">
        <div className="flex gap-2">
          <button
            onClick={() => handleStatusFilter(undefined)}
            className={`rounded-lg px-3 py-1.5 text-sm ${
              !filters.status
                ? 'bg-blue-600 text-white dark:bg-blue-500'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600'
            }`}
          >
            Todas
          </button>
          {(['PENDENTE', 'VENCIDO', 'PAGO', 'CANCELADO'] as StatusFatura[]).map((status) => (
            <button
              key={status}
              onClick={() => handleStatusFilter(status)}
              className={`rounded-lg px-3 py-1.5 text-sm ${
                filters.status === status
                  ? 'bg-blue-600 text-white dark:bg-blue-500'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600'
              }`}
            >
              {statusFaturaLabels[status]}
            </button>
          ))}
        </div>

        <div className="flex gap-2">
          <input
            type="date"
            className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-800 dark:text-gray-300"
            onChange={(e) => setFilters({ ...filters, dataInicio: e.target.value, page: 0 })}
          />
          <input
            type="date"
            className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-sm dark:border-gray-600 dark:bg-gray-800 dark:text-gray-300"
            onChange={(e) => setFilters({ ...filters, dataFim: e.target.value, page: 0 })}
          />
        </div>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-lg bg-white shadow dark:bg-gray-800">
        <table className="w-full">
          <thead className="bg-gray-50 dark:bg-gray-700">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500 dark:text-gray-400">Número</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500 dark:text-gray-400">Oficina</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500 dark:text-gray-400">Referência</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500 dark:text-gray-400">Vencimento</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500 dark:text-gray-400">Valor</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500 dark:text-gray-400">Status</th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500 dark:text-gray-400">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
            {isLoading ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-gray-500 dark:text-gray-400">
                  Carregando...
                </td>
              </tr>
            ) : faturas?.content.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-gray-500 dark:text-gray-400">
                  Nenhuma fatura encontrada
                </td>
              </tr>
            ) : (
              faturas?.content.map((fatura) => (
                <tr key={fatura.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <FileText className="h-4 w-4 text-gray-400 dark:text-gray-500" />
                      <span className="font-mono text-sm text-gray-900 dark:text-gray-100">{fatura.numero}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <Link
                      to={`/admin/oficinas/${fatura.oficinaId}`}
                      className="text-blue-600 hover:underline dark:text-blue-400"
                    >
                      {fatura.oficinaNome}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">
                    {fatura.mesReferenciaFormatado}
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <span className={fatura.vencida ? 'text-red-600 font-medium dark:text-red-400' : 'text-gray-600 dark:text-gray-400'}>
                      {formatDate(fatura.dataVencimento)}
                    </span>
                    {fatura.diasAteVencimento < 0 && (
                      <span className="ml-1 text-xs text-red-500 dark:text-red-400">
                        ({Math.abs(fatura.diasAteVencimento)} dias atrás)
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 font-medium text-gray-900 dark:text-gray-100">
                    {formatCurrency(fatura.valorTotal)}
                  </td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex rounded-full px-2 py-1 text-xs font-medium ${statusColors[fatura.status]}`}>
                      {fatura.statusLabel}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <Link
                        to={`/admin/faturas/${fatura.id}`}
                        className="rounded p-1 text-gray-500 hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-gray-200"
                        title="Ver detalhes"
                      >
                        <Eye className="h-4 w-4" />
                      </Link>
                      {fatura.pagavel && (
                        <button
                          onClick={() => handleRegistrarPagamento(fatura)}
                          className="rounded p-1 text-green-500 hover:bg-green-50 hover:text-green-700 dark:text-green-400 dark:hover:bg-green-900/30 dark:hover:text-green-300"
                          title="Registrar pagamento"
                        >
                          <DollarSign className="h-4 w-4" />
                        </button>
                      )}
                      {fatura.status === 'PENDENTE' && (
                        <button
                          onClick={() => handleCancelar(fatura)}
                          className="rounded p-1 text-red-500 hover:bg-red-50 hover:text-red-700 dark:text-red-400 dark:hover:bg-red-900/30 dark:hover:text-red-300"
                          title="Cancelar fatura"
                        >
                          <X className="h-4 w-4" />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Pagination */}
        {faturas && faturas.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-gray-200 bg-gray-50 px-4 py-3 dark:border-gray-700 dark:bg-gray-800">
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Mostrando {faturas.number * faturas.size + 1} a{' '}
              {Math.min((faturas.number + 1) * faturas.size, faturas.totalElements)} de{' '}
              {faturas.totalElements} resultados
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setFilters({ ...filters, page: (filters.page || 0) - 1 })}
                disabled={faturas.first}
                className="rounded-lg border border-gray-300 bg-white px-3 py-1 text-sm disabled:opacity-50 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-300"
              >
                Anterior
              </button>
              <button
                onClick={() => setFilters({ ...filters, page: (filters.page || 0) + 1 })}
                disabled={faturas.last}
                className="rounded-lg border border-gray-300 bg-white px-3 py-1 text-sm disabled:opacity-50 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-300"
              >
                Próximo
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Payment Modal */}
      {showPaymentModal && selectedFatura && (
        <RegistrarPagamentoModal
          fatura={selectedFatura}
          onClose={() => {
            setShowPaymentModal(false);
            setSelectedFatura(null);
          }}
          onSuccess={() => {
            setShowPaymentModal(false);
            setSelectedFatura(null);
          }}
        />
      )}
    </div>
  );
}
