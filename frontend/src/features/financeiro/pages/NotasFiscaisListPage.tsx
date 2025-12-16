/**
 * Página de listagem de Notas Fiscais
 */

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import {
  FileText,
  FilterX,
  Plus,
  Eye,
  Edit,
  Trash2,
} from 'lucide-react';
import { useNotasFiscais, useDeletarNotaFiscal } from '../hooks/useNotasFiscais';
import { NotaFiscalStatusBadge } from '../components/NotaFiscalStatusBadge';
import {
  TipoNotaFiscal,
  StatusNotaFiscal,
  TipoNotaFiscalLabels,
  StatusNotaFiscalLabels,
  type FiltrosNotaFiscal,
} from '../types/notaFiscal';

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
    // Verifica se a data é válida
    if (isNaN(date.getTime())) return '-';

    return format(date, 'dd/MM/yyyy', { locale: ptBR });
  } catch {
    return '-';
  }
};

export function NotasFiscaisListPage() {
  const [page, setPage] = useState(0);
  const [filtros, setFiltros] = useState<FiltrosNotaFiscal>({});

  const { data, isLoading, error } = useNotasFiscais(filtros, page, ITEMS_PER_PAGE);
  const deletarMutation = useDeletarNotaFiscal();

  const handleFiltroChange = (key: keyof FiltrosNotaFiscal, value: any) => {
    setFiltros((prev) => ({ ...prev, [key]: value || undefined }));
    setPage(0);
  };

  const limparFiltros = () => {
    setFiltros({});
    setPage(0);
  };

  const handleDeletar = async (id: string, numero: number) => {
    if (
      !window.confirm(
        `Tem certeza que deseja deletar a Nota Fiscal #${numero}? Esta ação não pode ser desfeita.`
      )
    )
      return;

    await deletarMutation.mutateAsync(id);
  };

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Erro ao carregar notas fiscais. Tente novamente.
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Notas Fiscais</h1>
          <p className="mt-1 text-sm text-gray-600">
            {data?.totalElements || 0} nota(s) fiscal(is) registrada(s)
          </p>
        </div>
        <Link
          to="/financeiro/notas-fiscais/novo"
          className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
        >
          <Plus className="h-5 w-5" />
          Nova Nota Fiscal
        </Link>
      </div>

      {/* Resumo cards */}
      {data && (
        <div className="mb-6 grid gap-4 md:grid-cols-4">
          <div className="rounded-lg bg-white p-6 shadow">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total de Notas</p>
                <p className="mt-2 text-2xl font-bold text-gray-900">
                  {data.totalElements}
                </p>
              </div>
              <div className="rounded-full bg-blue-100 p-3">
                <FileText className="h-6 w-6 text-blue-600" />
              </div>
            </div>
          </div>

          <div className="rounded-lg bg-white p-6 shadow">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Autorizadas</p>
                <p className="mt-2 text-2xl font-bold text-green-600">
                  {data.content.filter((nf) => nf.status === 'AUTORIZADA').length}
                </p>
              </div>
              <div className="rounded-full bg-green-100 p-3">
                <FileText className="h-6 w-6 text-green-600" />
              </div>
            </div>
          </div>

          <div className="rounded-lg bg-white p-6 shadow">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Em Digitação</p>
                <p className="mt-2 text-2xl font-bold text-gray-600">
                  {data.content.filter((nf) => nf.status === 'DIGITACAO').length}
                </p>
              </div>
              <div className="rounded-full bg-gray-100 p-3">
                <FileText className="h-6 w-6 text-gray-600" />
              </div>
            </div>
          </div>

          <div className="rounded-lg bg-white p-6 shadow">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Valor Total</p>
                <p className="mt-2 text-2xl font-bold text-gray-900">
                  {formatCurrency(
                    data.content.reduce((sum, nf) => sum + nf.valorTotal, 0)
                  )}
                </p>
              </div>
              <div className="rounded-full bg-purple-100 p-3">
                <FileText className="h-6 w-6 text-purple-600" />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Filtros */}
      <div className="mb-6 rounded-lg bg-white p-4 shadow">
        <div className="grid gap-4 md:grid-cols-4">
          {/* Tipo */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Tipo
            </label>
            <select
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={filtros.tipo || ''}
              onChange={(e) =>
                handleFiltroChange(
                  'tipo',
                  e.target.value === '' ? undefined : (e.target.value as TipoNotaFiscal)
                )
              }
            >
              <option value="">Todos</option>
              {Object.values(TipoNotaFiscal).map((tipo) => (
                <option key={tipo} value={tipo}>
                  {TipoNotaFiscalLabels[tipo]}
                </option>
              ))}
            </select>
          </div>

          {/* Status */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Status
            </label>
            <select
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={filtros.status || ''}
              onChange={(e) =>
                handleFiltroChange(
                  'status',
                  e.target.value === '' ? undefined : (e.target.value as StatusNotaFiscal)
                )
              }
            >
              <option value="">Todos</option>
              {Object.values(StatusNotaFiscal).map((status) => (
                <option key={status} value={status}>
                  {StatusNotaFiscalLabels[status]}
                </option>
              ))}
            </select>
          </div>

          {/* Data Início */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Data Início
            </label>
            <input
              type="date"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={filtros.dataInicio || ''}
              onChange={(e) => handleFiltroChange('dataInicio', e.target.value || undefined)}
            />
          </div>

          {/* Data Fim */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Data Fim
            </label>
            <input
              type="date"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
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
              className="flex items-center gap-2 rounded-lg border border-orange-300 bg-orange-50 px-4 py-2 text-orange-700 hover:bg-orange-100"
            >
              <FilterX className="h-4 w-4" />
              Limpar Filtros
            </button>
          </div>
        )}
      </div>

      {/* Tabela */}
      <div className="overflow-hidden rounded-lg bg-white shadow">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  Número
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  Série
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  Tipo
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  Data Emissão
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  Valor
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  Status
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700">
                  Ações
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center">
                    <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-blue-600 border-t-transparent"></div>
                  </td>
                </tr>
              ) : !data || data.content.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-gray-500">
                    Nenhuma nota fiscal encontrada
                  </td>
                </tr>
              ) : (
                data.content.map((notaFiscal) => (
                  <tr key={notaFiscal.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 text-sm font-semibold text-gray-900">
                      {notaFiscal.numero}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900">
                      {notaFiscal.serie}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900">
                      {TipoNotaFiscalLabels[notaFiscal.tipo]}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900">
                      {formatDate(notaFiscal.dataEmissao)}
                    </td>
                    <td className="px-6 py-4 text-sm font-semibold text-gray-900">
                      {formatCurrency(notaFiscal.valorTotal)}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      <NotaFiscalStatusBadge status={notaFiscal.status} />
                    </td>
                    <td className="px-6 py-4 text-right text-sm">
                      <div className="flex justify-end gap-2">
                        <Link
                          to={`/financeiro/notas-fiscais/${notaFiscal.id}`}
                          className="rounded p-1 text-blue-600 hover:bg-blue-50"
                          title="Ver detalhes"
                        >
                          <Eye className="h-4 w-4" />
                        </Link>
                        {notaFiscal.status === 'DIGITACAO' && (
                          <>
                            <Link
                              to={`/financeiro/notas-fiscais/${notaFiscal.id}/editar`}
                              className="rounded p-1 text-green-600 hover:bg-green-50"
                              title="Editar"
                            >
                              <Edit className="h-4 w-4" />
                            </Link>
                            <button
                              onClick={() =>
                                handleDeletar(notaFiscal.id, notaFiscal.numero)
                              }
                              disabled={deletarMutation.isPending}
                              className="rounded p-1 text-red-600 hover:bg-red-50 disabled:opacity-50"
                              title="Deletar"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Paginação */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-gray-200 bg-white px-6 py-4">
            <div className="text-sm text-gray-700">
              Mostrando {page * ITEMS_PER_PAGE + 1} a{' '}
              {Math.min((page + 1) * ITEMS_PER_PAGE, data.totalElements)} de{' '}
              {data.totalElements} resultados
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Anterior
              </button>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= data.totalPages - 1}
                className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Próxima
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
