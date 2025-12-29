import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertTriangle,
  Building2,
  FileText,
  TrendingUp,
  Bell,
  Ban,
  RefreshCw,
  Eye,
  Handshake,
  ChevronRight,
} from 'lucide-react';
import { useInadimplenciaDashboard, useInadimplentes, useExecutarAcaoMassa } from '../hooks/useSaas';
import type { OficinaInadimplente, AcaoInadimplencia } from '../types';
import { acaoInadimplenciaLabels } from '../types';
import { CriarAcordoModal } from '../components/CriarAcordoModal';

export function InadimplenciaPage() {
  const [selectedOficinas, setSelectedOficinas] = useState<string[]>([]);
  const [showAcordoModal, setShowAcordoModal] = useState(false);
  const [selectedOficinaForAcordo, setSelectedOficinaForAcordo] = useState<OficinaInadimplente | null>(null);

  const { data: dashboard, isLoading: loadingDashboard } = useInadimplenciaDashboard();
  const { data: inadimplentes, isLoading: loadingList } = useInadimplentes(0, 20);
  const executarAcao = useExecutarAcaoMassa();

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  };

  const handleSelectAll = (checked: boolean) => {
    if (checked && inadimplentes) {
      setSelectedOficinas(inadimplentes.content.map((o) => o.oficinaId));
    } else {
      setSelectedOficinas([]);
    }
  };

  const handleSelectOficina = (id: string, checked: boolean) => {
    if (checked) {
      setSelectedOficinas([...selectedOficinas, id]);
    } else {
      setSelectedOficinas(selectedOficinas.filter((o) => o !== id));
    }
  };

  const handleAcaoMassa = async (acao: AcaoInadimplencia) => {
    if (selectedOficinas.length === 0) {
      alert('Selecione pelo menos uma oficina');
      return;
    }

    if (!confirm(`Deseja executar a ação "${acaoInadimplenciaLabels[acao]}" em ${selectedOficinas.length} oficina(s)?`)) {
      return;
    }

    try {
      const result = await executarAcao.mutateAsync({
        oficinaIds: selectedOficinas,
        acao,
      });
      alert(`Ação executada: ${result.totalSucesso} sucesso, ${result.totalFalha} falhas`);
      setSelectedOficinas([]);
    } catch (error) {
      console.error('Erro ao executar ação:', error);
      alert('Erro ao executar ação em massa');
    }
  };

  const handleCriarAcordo = (oficina: OficinaInadimplente) => {
    setSelectedOficinaForAcordo(oficina);
    setShowAcordoModal(true);
  };

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Gestão de Inadimplência</h1>
          <p className="text-gray-600 dark:text-gray-400">Monitore e gerencie oficinas com faturas em atraso</p>
        </div>
        <Link
          to="/admin/inadimplencia/acordos"
          className="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700"
        >
          <Handshake className="h-4 w-4" />
          Ver Acordos
        </Link>
      </div>

      {/* Dashboard Cards */}
      {loadingDashboard ? (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-28 animate-pulse rounded-lg bg-gray-200 dark:bg-gray-700" />
          ))}
        </div>
      ) : dashboard && (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          <div className="rounded-lg border border-red-200 bg-red-50 p-4 dark:border-red-800 dark:bg-red-900/20">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-red-100 p-2 dark:bg-red-800/30">
                <AlertTriangle className="h-5 w-5 text-red-600 dark:text-red-400" />
              </div>
              <div>
                <p className="text-sm text-red-700 dark:text-red-400">Valor Inadimplente</p>
                <p className="text-xl font-bold text-red-900 dark:text-red-300">
                  {formatCurrency(dashboard.valorTotalInadimplente)}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-lg border border-orange-200 bg-orange-50 p-4 dark:border-orange-800 dark:bg-orange-900/20">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-orange-100 p-2 dark:bg-orange-800/30">
                <Building2 className="h-5 w-5 text-orange-600 dark:text-orange-400" />
              </div>
              <div>
                <p className="text-sm text-orange-700 dark:text-orange-400">Oficinas Inadimplentes</p>
                <p className="text-xl font-bold text-orange-900 dark:text-orange-300">{dashboard.oficinasInadimplentes}</p>
              </div>
            </div>
          </div>

          <div className="rounded-lg border border-yellow-200 bg-yellow-50 p-4 dark:border-yellow-800 dark:bg-yellow-900/20">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-yellow-100 p-2 dark:bg-yellow-800/30">
                <FileText className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />
              </div>
              <div>
                <p className="text-sm text-yellow-700 dark:text-yellow-400">Faturas Vencidas</p>
                <p className="text-xl font-bold text-yellow-900 dark:text-yellow-300">{dashboard.faturasVencidas}</p>
              </div>
            </div>
          </div>

          <div className="rounded-lg border border-green-200 bg-green-50 p-4 dark:border-green-800 dark:bg-green-900/20">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-green-100 p-2 dark:bg-green-800/30">
                <TrendingUp className="h-5 w-5 text-green-600 dark:text-green-400" />
              </div>
              <div>
                <p className="text-sm text-green-700 dark:text-green-400">Recuperado no Mês</p>
                <p className="text-xl font-bold text-green-900 dark:text-green-300">
                  {formatCurrency(dashboard.valorRecuperadoMes)}
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Breakdown by Days */}
      {dashboard && dashboard.porFaixaAtraso && (
        <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">Por Faixa de Atraso</h2>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
            {Object.entries(dashboard.porFaixaAtraso).map(([key, faixa]) => (
              <div key={key} className="rounded-lg border border-gray-200 p-3 dark:border-gray-700">
                <p className="text-sm font-medium text-gray-700 dark:text-gray-300">{faixa.faixa}</p>
                <p className="text-xl font-bold text-gray-900 dark:text-white">{formatCurrency(faixa.valorTotal)}</p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  {faixa.quantidadeFaturas} faturas • {faixa.quantidadeOficinas} oficinas
                </p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Mass Actions */}
      {selectedOficinas.length > 0 && (
        <div className="sticky top-0 z-10 flex items-center justify-between rounded-lg border border-blue-200 bg-blue-50 p-4 dark:border-blue-800 dark:bg-blue-900/20">
          <p className="font-medium text-blue-900 dark:text-blue-300">
            {selectedOficinas.length} oficina(s) selecionada(s)
          </p>
          <div className="flex gap-2">
            <button
              onClick={() => handleAcaoMassa('NOTIFICAR')}
              disabled={executarAcao.isPending}
              className="flex items-center gap-1 rounded-lg bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
            >
              <Bell className="h-4 w-4" />
              Notificar
            </button>
            <button
              onClick={() => handleAcaoMassa('SUSPENDER')}
              disabled={executarAcao.isPending}
              className="flex items-center gap-1 rounded-lg bg-orange-600 px-3 py-1.5 text-sm text-white hover:bg-orange-700 disabled:opacity-50"
            >
              <Ban className="h-4 w-4" />
              Suspender
            </button>
            <button
              onClick={() => setSelectedOficinas([])}
              className="flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              Limpar Seleção
            </button>
          </div>
        </div>
      )}

      {/* Defaulters Table */}
      <div className="overflow-hidden rounded-lg bg-white shadow dark:bg-gray-800">
        <div className="border-b border-gray-200 px-4 py-3 dark:border-gray-700">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Oficinas Inadimplentes</h2>
        </div>
        <table className="w-full">
          <thead className="bg-gray-50 dark:bg-gray-700">
            <tr>
              <th className="px-4 py-3 text-left">
                <input
                  type="checkbox"
                  checked={inadimplentes?.content.length === selectedOficinas.length && selectedOficinas.length > 0}
                  onChange={(e) => handleSelectAll(e.target.checked)}
                  className="rounded border-gray-300 dark:border-gray-600"
                />
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500 dark:text-gray-400">Oficina</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500 dark:text-gray-400">Faturas</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500 dark:text-gray-400">Valor Devido</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500 dark:text-gray-400">Maior Atraso</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500 dark:text-gray-400">Status</th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500 dark:text-gray-400">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
            {loadingList ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-gray-500 dark:text-gray-400">
                  <RefreshCw className="mx-auto h-6 w-6 animate-spin" />
                </td>
              </tr>
            ) : inadimplentes?.content.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-gray-500 dark:text-gray-400">
                  Nenhuma oficina inadimplente
                </td>
              </tr>
            ) : (
              inadimplentes?.content.map((oficina) => (
                <tr key={oficina.oficinaId} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                  <td className="px-4 py-3">
                    <input
                      type="checkbox"
                      checked={selectedOficinas.includes(oficina.oficinaId)}
                      onChange={(e) => handleSelectOficina(oficina.oficinaId, e.target.checked)}
                      className="rounded border-gray-300 dark:border-gray-600"
                    />
                  </td>
                  <td className="px-4 py-3">
                    <div>
                      <p className="font-medium text-gray-900 dark:text-white">{oficina.nomeFantasia}</p>
                      <p className="text-sm text-gray-500 dark:text-gray-400">{oficina.cnpj}</p>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <span className="inline-flex rounded-full bg-red-100 px-2 py-1 text-xs font-medium text-red-800 dark:bg-red-900/30 dark:text-red-400">
                      {oficina.faturasVencidas} vencida(s)
                    </span>
                  </td>
                  <td className="px-4 py-3 font-medium text-gray-900 dark:text-white">
                    {formatCurrency(oficina.valorTotalDevido)}
                  </td>
                  <td className="px-4 py-3">
                    <span className={`font-medium ${
                      oficina.diasAtrasoMaior > 60 ? 'text-red-600 dark:text-red-400' :
                      oficina.diasAtrasoMaior > 30 ? 'text-orange-600 dark:text-orange-400' :
                      'text-yellow-600 dark:text-yellow-400'
                    }`}>
                      {oficina.diasAtrasoMaior} dias
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    {oficina.possuiAcordoAtivo ? (
                      <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2 py-1 text-xs font-medium text-green-800 dark:bg-green-900/30 dark:text-green-400">
                        <Handshake className="h-3 w-3" />
                        Acordo Ativo
                      </span>
                    ) : (
                      <span className="inline-flex rounded-full bg-gray-100 px-2 py-1 text-xs text-gray-600 dark:bg-gray-700 dark:text-gray-400">
                        Sem acordo
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <Link
                        to={`/admin/oficinas/${oficina.oficinaId}`}
                        className="rounded p-1 text-gray-500 hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-gray-200"
                        title="Ver oficina"
                      >
                        <Eye className="h-4 w-4" />
                      </Link>
                      {!oficina.possuiAcordoAtivo && (
                        <button
                          onClick={() => handleCriarAcordo(oficina)}
                          className="rounded p-1 text-green-500 hover:bg-green-50 hover:text-green-700 dark:text-green-400 dark:hover:bg-green-900/30 dark:hover:text-green-300"
                          title="Criar acordo"
                        >
                          <Handshake className="h-4 w-4" />
                        </button>
                      )}
                      <Link
                        to={`/admin/faturas?oficinaId=${oficina.oficinaId}`}
                        className="rounded p-1 text-blue-500 hover:bg-blue-50 hover:text-blue-700 dark:text-blue-400 dark:hover:bg-blue-900/30 dark:hover:text-blue-300"
                        title="Ver faturas"
                      >
                        <ChevronRight className="h-4 w-4" />
                      </Link>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Create Agreement Modal */}
      {showAcordoModal && selectedOficinaForAcordo && (
        <CriarAcordoModal
          oficina={selectedOficinaForAcordo}
          onClose={() => {
            setShowAcordoModal(false);
            setSelectedOficinaForAcordo(null);
          }}
          onSuccess={() => {
            setShowAcordoModal(false);
            setSelectedOficinaForAcordo(null);
          }}
        />
      )}
    </div>
  );
}
