import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Plus, Search, Wrench, AlertTriangle, CheckCircle, Pause } from 'lucide-react';
import { usePlanos, useTiposManutencao } from '../hooks/useManutencaoPreventiva';
import type { PlanoManutencao, StatusPlanoManutencao } from '../types';

export default function PlanosListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [busca, setBusca] = useState(searchParams.get('busca') || '');

  const filters = {
    status: searchParams.get('status') as StatusPlanoManutencao | undefined,
    tipoManutencao: searchParams.get('tipo') || undefined,
    busca: searchParams.get('busca') || undefined,
    page: parseInt(searchParams.get('page') || '0'),
    size: 20,
  };

  const { data: planosData, isLoading, error } = usePlanos(filters);
  const { data: tiposManutencao } = useTiposManutencao();

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const params = new URLSearchParams(searchParams);
    if (busca) {
      params.set('busca', busca);
    } else {
      params.delete('busca');
    }
    params.set('page', '0');
    setSearchParams(params);
  };

  const handleFilterChange = (key: string, value: string) => {
    const params = new URLSearchParams(searchParams);
    if (value) {
      params.set(key, value);
    } else {
      params.delete(key);
    }
    params.set('page', '0');
    setSearchParams(params);
  };

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
            Planos de Manutenção
          </h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 hidden sm:block">
            Gerencie os planos de manutenção preventiva dos veículos
          </p>
        </div>
        <Link
          to="/manutencao-preventiva/novo"
          className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          <Plus className="h-4 w-4" />
          <span>Novo Plano</span>
        </Link>
      </div>

      {/* Filters */}
      <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
        <div className="flex flex-col md:flex-row gap-4">
          {/* Search */}
          <form onSubmit={handleSearch} className="flex-1">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
              <input
                type="text"
                placeholder="Buscar por nome ou placa..."
                defaultValue={busca}
                onChange={(e) => setBusca(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
              />
            </div>
          </form>

          {/* Status Filter */}
          <select
            defaultValue={filters.status || ''}
            onChange={(e) => handleFilterChange('status', e.target.value)}
            className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
          >
            <option value="">Todos os status</option>
            <option value="ATIVO">Ativos</option>
            <option value="PAUSADO">Pausados</option>
            <option value="VENCIDO">Vencidos</option>
            <option value="CONCLUIDO">Concluídos</option>
          </select>

          {/* Tipo Filter */}
          <select
            defaultValue={filters.tipoManutencao || ''}
            onChange={(e) => handleFilterChange('tipo', e.target.value)}
            className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
          >
            <option value="">Todos os tipos</option>
            {tiposManutencao?.map((tipo) => (
              <option key={tipo} value={tipo}>{tipo}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      ) : error ? (
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Erro ao carregar planos. Tente novamente.
        </div>
      ) : planosData?.content.length === 0 ? (
        <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-8 text-center">
          <Wrench className="h-12 w-12 mx-auto text-gray-400 mb-4" />
          <p className="text-gray-600 dark:text-gray-400 mb-4">
            Nenhum plano de manutenção encontrado
          </p>
          <Link
            to="/manutencao-preventiva/novo"
            className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            <Plus className="h-4 w-4" />
            Criar primeiro plano
          </Link>
        </div>
      ) : (
        <>
          {/* Desktop Table */}
          <div className="hidden lg:block bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 dark:text-gray-300 uppercase">
                    Plano
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 dark:text-gray-300 uppercase">
                    Veículo
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 dark:text-gray-300 uppercase">
                    Tipo
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 dark:text-gray-300 uppercase">
                    Próxima
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 dark:text-gray-300 uppercase">
                    Status
                  </th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-gray-700 dark:text-gray-300 uppercase">
                    Ações
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {planosData?.content.map((plano) => (
                  <PlanoTableRow key={plano.id} plano={plano} />
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile Cards */}
          <div className="lg:hidden space-y-4">
            {planosData?.content.map((plano) => (
              <PlanoCard key={plano.id} plano={plano} />
            ))}
          </div>

          {/* Pagination */}
          {planosData && planosData.totalPages > 1 && (
            <div className="flex flex-col sm:flex-row items-center justify-center gap-2 sm:gap-3">
              <button
                onClick={() => handleFilterChange('page', String(filters.page - 1))}
                disabled={planosData.first}
                className="w-full sm:w-auto px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg disabled:opacity-50 text-gray-700 dark:text-gray-300"
              >
                Anterior
              </button>
              <span className="px-4 py-2 text-sm sm:text-base text-gray-600 dark:text-gray-400">
                {filters.page + 1} de {planosData.totalPages}
              </span>
              <button
                onClick={() => handleFilterChange('page', String(filters.page + 1))}
                disabled={planosData.last}
                className="w-full sm:w-auto px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg disabled:opacity-50 text-gray-700 dark:text-gray-300"
              >
                Próximo
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function PlanoTableRow({ plano }: { plano: PlanoManutencao }) {
  return (
    <tr className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
      <td className="px-4 py-4">
        <Link to={`/manutencao-preventiva/${plano.id}`} className="hover:text-blue-600">
          <p className="font-medium text-gray-900 dark:text-white">{plano.nome}</p>
          {plano.descricao && (
            <p className="text-sm text-gray-500 dark:text-gray-400 truncate max-w-xs">{plano.descricao}</p>
          )}
        </Link>
      </td>
      <td className="px-4 py-4">
        <p className="text-gray-900 dark:text-white">{plano.veiculo.placaFormatada}</p>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          {plano.veiculo.marca} {plano.veiculo.modelo}
        </p>
      </td>
      <td className="px-4 py-4 text-gray-900 dark:text-white">
        {plano.tipoManutencao}
      </td>
      <td className="px-4 py-4">
        <ProximaPrevisao plano={plano} />
      </td>
      <td className="px-4 py-4">
        <StatusBadge status={plano.status} vencido={plano.vencido} />
      </td>
      <td className="px-4 py-4 text-right">
        <Link
          to={`/manutencao-preventiva/${plano.id}`}
          className="text-blue-600 dark:text-blue-400 hover:underline"
        >
          Ver detalhes
        </Link>
      </td>
    </tr>
  );
}

function PlanoCard({ plano }: { plano: PlanoManutencao }) {
  return (
    <Link
      to={`/manutencao-preventiva/${plano.id}`}
      className="block bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4 hover:border-blue-500"
    >
      <div className="flex items-start justify-between mb-2">
        <div>
          <p className="font-medium text-gray-900 dark:text-white">{plano.nome}</p>
          <p className="text-sm text-gray-500 dark:text-gray-400">{plano.tipoManutencao}</p>
        </div>
        <StatusBadge status={plano.status} vencido={plano.vencido} />
      </div>
      <div className="flex items-center justify-between text-sm">
        <span className="text-gray-600 dark:text-gray-400">
          {plano.veiculo.placaFormatada} - {plano.veiculo.marca} {plano.veiculo.modelo}
        </span>
        <ProximaPrevisao plano={plano} />
      </div>
    </Link>
  );
}

function ProximaPrevisao({ plano }: { plano: PlanoManutencao }) {
  if (plano.vencido) {
    return (
      <span className="text-red-600 dark:text-red-400 font-medium">
        Vencido
      </span>
    );
  }
  if (plano.proximoAVencer) {
    return (
      <span className="text-yellow-600 dark:text-yellow-400 font-medium">
        {plano.diasParaVencer}d
      </span>
    );
  }
  if (plano.proximaPrevisaoData) {
    return (
      <span className="text-gray-600 dark:text-gray-400">
        {new Date(plano.proximaPrevisaoData).toLocaleDateString('pt-BR')}
      </span>
    );
  }
  if (plano.proximaPrevisaoKm) {
    return (
      <span className="text-gray-600 dark:text-gray-400">
        {plano.proximaPrevisaoKm.toLocaleString()} km
      </span>
    );
  }
  return <span className="text-gray-400">-</span>;
}

function StatusBadge({ status, vencido }: { status: StatusPlanoManutencao; vencido: boolean }) {
  if (vencido && status === 'ATIVO') {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-1 text-xs font-medium rounded-full bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400">
        <AlertTriangle className="h-3 w-3" />
        Vencido
      </span>
    );
  }

  const styles = {
    ATIVO: 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400',
    PAUSADO: 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400',
    CONCLUIDO: 'bg-gray-100 dark:bg-gray-900/30 text-gray-600 dark:text-gray-400',
    VENCIDO: 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
  };

  const icons = {
    ATIVO: <CheckCircle className="h-3 w-3" />,
    PAUSADO: <Pause className="h-3 w-3" />,
    CONCLUIDO: <CheckCircle className="h-3 w-3" />,
    VENCIDO: <AlertTriangle className="h-3 w-3" />,
  };

  return (
    <span className={`inline-flex items-center gap-1 px-2 py-1 text-xs font-medium rounded-full ${styles[status]}`}>
      {icons[status]}
      {status}
    </span>
  );
}
