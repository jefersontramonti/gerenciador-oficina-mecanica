import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, AlertTriangle, Calendar, Car, ChevronRight, RefreshCw, Eye, Play } from 'lucide-react';
import { usePlanosVencidos } from '../hooks/useManutencaoPreventiva';
import type { PlanoManutencao } from '../types';

export default function VencidosListPage() {
  const navigate = useNavigate();
  const { data: planosVencidos, isLoading, error, refetch } = usePlanosVencidos();

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3 sm:gap-4">
          <Link
            to="/manutencao-preventiva"
            className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <ArrowLeft className="h-5 w-5 text-gray-500 dark:text-gray-400" />
          </Link>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
              <AlertTriangle className="h-5 w-5 sm:h-6 sm:w-6 text-red-500" />
              <span className="hidden sm:inline">Manutenções Vencidas</span>
              <span className="sm:hidden">Vencidas</span>
            </h1>
            <p className="text-sm text-gray-600 dark:text-gray-400 hidden sm:block">
              Planos de manutenção que já passaram da data prevista
            </p>
          </div>
        </div>
        <button
          onClick={() => refetch()}
          className="flex items-center justify-center gap-2 px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 w-full sm:w-auto"
        >
          <RefreshCw className="h-4 w-4" />
          <span>Atualizar</span>
        </button>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      ) : error ? (
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Erro ao carregar planos vencidos. Tente novamente.
        </div>
      ) : !planosVencidos || planosVencidos.length === 0 ? (
        <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-6 sm:p-8 text-center">
          <div className="flex justify-center mb-4">
            <div className="p-3 rounded-full bg-green-100 dark:bg-green-900/30">
              <AlertTriangle className="h-6 w-6 sm:h-8 sm:w-8 text-green-600 dark:text-green-400" />
            </div>
          </div>
          <h3 className="text-base sm:text-lg font-medium text-gray-900 dark:text-white mb-2">
            Nenhuma manutenção vencida
          </h3>
          <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
            Todos os planos de manutenção estão em dia.
          </p>
          <Link
            to="/manutencao-preventiva"
            className="inline-flex items-center gap-2 text-blue-600 dark:text-blue-400 hover:underline text-sm"
          >
            Voltar ao dashboard
            <ChevronRight className="h-4 w-4" />
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Summary */}
          <div className="bg-red-50 dark:bg-red-900/20 rounded-lg border border-red-200 dark:border-red-800 p-3 sm:p-4">
            <p className="text-sm sm:text-base text-red-800 dark:text-red-300">
              <strong>{planosVencidos.length}</strong> plano{planosVencidos.length !== 1 ? 's' : ''} de manutenção vencido{planosVencidos.length !== 1 ? 's' : ''}
            </p>
          </div>

          {/* Mobile Cards */}
          <div className="lg:hidden space-y-3">
            {planosVencidos.map((plano) => (
              <MobileCard key={plano.id} plano={plano} navigate={navigate} />
            ))}
          </div>

          {/* Desktop Table */}
          <div className="hidden lg:block bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Atraso
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Plano
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Veículo
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Tipo
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Data Prevista
                  </th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Ações
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {planosVencidos.map((plano) => (
                  <DesktopRow key={plano.id} plano={plano} navigate={navigate} />
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

function MobileCard({ plano, navigate }: { plano: PlanoManutencao; navigate: (path: string) => void }) {
  const diasVencido = plano.diasParaVencer ? Math.abs(plano.diasParaVencer) : 0;

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4 shadow-sm">
      <div className="flex items-start justify-between mb-3">
        <div className="flex-1 min-w-0">
          <h3 className="font-medium text-gray-900 dark:text-white truncate">
            {plano.nome}
          </h3>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {plano.tipoManutencao}
          </p>
        </div>
        <span className="inline-flex items-center px-2 py-1 text-xs font-medium rounded-full bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 ml-2">
          <AlertTriangle className="h-3 w-3 mr-1" />
          {diasVencido}d
        </span>
      </div>

      <div className="space-y-2 text-sm mb-3">
        {plano.veiculo && (
          <div className="flex items-center gap-2 text-gray-600 dark:text-gray-400">
            <Car className="h-4 w-4 flex-shrink-0" />
            <span className="truncate">
              {plano.veiculo.placaFormatada || plano.veiculo.placa} - {plano.veiculo.marca} {plano.veiculo.modelo}
            </span>
          </div>
        )}
        {plano.proximaPrevisaoData && (
          <div className="flex items-center gap-2 text-gray-600 dark:text-gray-400">
            <Calendar className="h-4 w-4 flex-shrink-0" />
            <span>Previsto: {plano.proximaPrevisaoData}</span>
          </div>
        )}
      </div>

      <div className="flex gap-2 pt-3 border-t border-gray-200 dark:border-gray-700">
        <button
          onClick={() => navigate(`/manutencao-preventiva/${plano.id}`)}
          className="flex-1 flex items-center justify-center gap-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 px-3 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
        >
          <Eye className="h-4 w-4" />
          Ver
        </button>
        <button
          onClick={() => navigate(`/manutencao-preventiva/${plano.id}`)}
          className="flex-1 flex items-center justify-center gap-1 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          <Play className="h-4 w-4" />
          Executar
        </button>
      </div>
    </div>
  );
}

function DesktopRow({ plano, navigate }: { plano: PlanoManutencao; navigate: (path: string) => void }) {
  const diasVencido = plano.diasParaVencer ? Math.abs(plano.diasParaVencer) : 0;

  return (
    <tr className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
      <td className="px-4 py-4 whitespace-nowrap">
        <span className="inline-flex items-center px-2 py-1 text-xs font-medium rounded-full bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400">
          <AlertTriangle className="h-3 w-3 mr-1" />
          {diasVencido}d atrasado
        </span>
      </td>
      <td className="px-4 py-4">
        <p className="font-medium text-gray-900 dark:text-white">{plano.nome}</p>
      </td>
      <td className="px-4 py-4 text-sm text-gray-500 dark:text-gray-400">
        {plano.veiculo ? (
          <>
            {plano.veiculo.placaFormatada || plano.veiculo.placa}
            <br />
            <span className="text-xs">{plano.veiculo.marca} {plano.veiculo.modelo}</span>
          </>
        ) : (
          <span className="italic">-</span>
        )}
      </td>
      <td className="px-4 py-4 text-sm text-gray-500 dark:text-gray-400">
        {plano.tipoManutencao}
      </td>
      <td className="px-4 py-4 text-sm text-gray-500 dark:text-gray-400">
        {plano.proximaPrevisaoData || '-'}
      </td>
      <td className="px-4 py-4 text-right">
        <div className="flex justify-end gap-2">
          <button
            onClick={() => navigate(`/manutencao-preventiva/${plano.id}`)}
            className="p-2 rounded-lg text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700"
            title="Visualizar"
          >
            <Eye className="h-4 w-4" />
          </button>
          <button
            onClick={() => navigate(`/manutencao-preventiva/${plano.id}`)}
            className="p-2 rounded-lg text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20"
            title="Executar"
          >
            <Play className="h-4 w-4" />
          </button>
        </div>
      </td>
    </tr>
  );
}
