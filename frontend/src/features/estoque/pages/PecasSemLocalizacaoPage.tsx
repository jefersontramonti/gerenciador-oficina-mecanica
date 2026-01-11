/**
 * Página para listar e gerenciar peças sem localização definida
 */

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { MapPinOff, Package, ExternalLink, MapPin } from 'lucide-react';
import { usePecasSemLocalizacao, useDefinirLocalizacao } from '../hooks/usePecas';
import { UnidadeMedidaSigla, getStockStatus } from '../types';
import { formatCurrency } from '@/shared/utils/formatters';
import { LocalArmazenamentoSelect } from '../components';

export const PecasSemLocalizacaoPage = () => {
  const [page] = useState(0);
  const size = 50; // Mostrar mais itens por página

  const { data, isLoading } = usePecasSemLocalizacao(page, size);
  const definirLocalizacao = useDefinirLocalizacao();

  const [selectedPecaId, setSelectedPecaId] = useState<string | null>(null);
  const [selectedLocalId, setSelectedLocalId] = useState<string>('');

  const pecas = data?.content || [];

  const handleAssignLocation = async (pecaId: string) => {
    if (!selectedLocalId) {
      alert('Selecione um local de armazenamento');
      return;
    }

    try {
      await definirLocalizacao.mutateAsync({
        pecaId,
        localId: selectedLocalId,
      });
      // Reset selection
      setSelectedPecaId(null);
      setSelectedLocalId('');
    } catch (error) {
      // Error handled by mutation
    }
  };

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <div className="rounded-full bg-orange-100 dark:bg-orange-900/30 p-2 shrink-0">
            <MapPinOff className="h-5 w-5 sm:h-6 sm:w-6 text-orange-600 dark:text-orange-400" />
          </div>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">Peças Sem Localização</h1>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              Gerencie peças que ainda não possuem um local de armazenamento definido
            </p>
          </div>
        </div>
      </div>

      {/* Alert Banner */}
      {pecas.length > 0 && (
        <div className="mb-6 rounded-lg border border-orange-200 dark:border-orange-800 bg-orange-50 dark:bg-orange-900/20 p-4">
          <div className="flex items-start gap-3">
            <MapPinOff className="h-5 w-5 text-orange-600 dark:text-orange-400 mt-0.5" />
            <div className="flex-1">
              <h3 className="font-semibold text-orange-900 dark:text-orange-200">
                {pecas.length} {pecas.length === 1 ? 'peça sem localização' : 'peças sem localização'}
              </h3>
              <p className="text-sm text-orange-800 dark:text-orange-300 mt-1">
                Defina locais de armazenamento para facilitar a organização e localização das peças
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Lista de Peças */}
      <div className="rounded-lg bg-white dark:bg-gray-800 shadow overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center items-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        ) : pecas.length === 0 ? (
          <div className="text-center py-12 px-4">
            <div className="rounded-full bg-green-100 dark:bg-green-900/30 w-16 h-16 flex items-center justify-center mx-auto mb-4">
              <Package className="h-8 w-8 text-green-600 dark:text-green-400" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">Todas as peças têm localização</h3>
            <p className="text-gray-500 dark:text-gray-400">
              Não há peças sem localização definida no momento.
            </p>
          </div>
        ) : (
          <>
            {/* Mobile: Card Layout */}
            <div className="space-y-3 p-4 lg:hidden">
              {pecas.map((peca) => {
                const status = getStockStatus(peca.quantidadeAtual, peca.quantidadeMinima);
                const isEditing = selectedPecaId === peca.id;

                return (
                  <div key={peca.id} className="rounded-lg border border-gray-200 dark:border-gray-700 p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="font-mono text-sm font-medium text-gray-900 dark:text-white">
                            {peca.codigo}
                          </span>
                          <span
                            className={`px-2 py-0.5 rounded-full text-xs font-medium ${status.bgColor} ${status.textColor}`}
                          >
                            {status.label}
                          </span>
                        </div>
                        <p className="mt-1 text-sm text-gray-900 dark:text-white truncate">
                          {peca.descricao}
                        </p>
                        {peca.marca && (
                          <p className="text-xs text-gray-500 dark:text-gray-400">
                            Marca: {peca.marca}
                          </p>
                        )}
                      </div>
                      <Link
                        to={`/estoque/${peca.id}`}
                        className="shrink-0 p-2 text-blue-600 dark:text-blue-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
                      >
                        <ExternalLink className="h-4 w-4" />
                      </Link>
                    </div>

                    <div className="mt-3 grid grid-cols-2 gap-2 text-sm">
                      <div>
                        <span className="text-gray-500 dark:text-gray-400">Qtd:</span>
                        <span className="ml-1 font-medium text-gray-900 dark:text-white">
                          {peca.quantidadeAtual} {UnidadeMedidaSigla[peca.unidadeMedida]}
                        </span>
                      </div>
                      <div>
                        <span className="text-gray-500 dark:text-gray-400">Valor:</span>
                        <span className="ml-1 font-medium text-gray-900 dark:text-white">
                          {formatCurrency(peca.valorVenda)}
                        </span>
                      </div>
                    </div>

                    <div className="mt-3 pt-3 border-t border-gray-200 dark:border-gray-700">
                      <div className="text-sm text-orange-600 dark:text-orange-400 font-medium mb-2">
                        Local não definido
                      </div>
                      {isEditing ? (
                        <div className="space-y-2">
                          <LocalArmazenamentoSelect
                            value={selectedLocalId}
                            onChange={setSelectedLocalId}
                            placeholder="Selecione o local"
                            allowEmpty={false}
                          />
                          <div className="flex gap-2">
                            <button
                              onClick={() => handleAssignLocation(peca.id)}
                              disabled={!selectedLocalId || definirLocalizacao.isPending}
                              className="flex-1 inline-flex items-center justify-center gap-1 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                              <MapPin className="h-3 w-3" />
                              Salvar
                            </button>
                            <button
                              onClick={() => {
                                setSelectedPecaId(null);
                                setSelectedLocalId('');
                              }}
                              className="flex-1 rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                            >
                              Cancelar
                            </button>
                          </div>
                        </div>
                      ) : (
                        <button
                          onClick={() => setSelectedPecaId(peca.id)}
                          className="w-full inline-flex items-center justify-center gap-1 rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                        >
                          <MapPin className="h-3 w-3" />
                          Definir Local
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Desktop: Table Layout */}
            <div className="hidden lg:block overflow-x-auto">
              <table className="w-full divide-y divide-gray-200 dark:divide-gray-700">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Código
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Descrição
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Marca
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Qtd. Atual
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Status
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Valor
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Local
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Ações
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-800">
                  {pecas.map((peca) => {
                    const status = getStockStatus(peca.quantidadeAtual, peca.quantidadeMinima);
                    const isEditing = selectedPecaId === peca.id;

                    return (
                      <tr key={peca.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                        <td className="px-6 py-4 text-sm font-mono text-gray-900 dark:text-white">
                          {peca.codigo}
                        </td>
                        <td className="px-6 py-4">
                          <div>
                            <p className="font-medium text-gray-900 dark:text-white">{peca.descricao}</p>
                            {peca.aplicacao && (
                              <p className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-xs">
                                {peca.aplicacao}
                              </p>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-900 dark:text-white">
                          {peca.marca || '-'}
                        </td>
                        <td className="px-6 py-4 text-sm text-right font-medium text-gray-900 dark:text-white">
                          {peca.quantidadeAtual} {UnidadeMedidaSigla[peca.unidadeMedida]}
                        </td>
                        <td className="px-6 py-4">
                          <span
                            className={`px-2 py-1 rounded-full text-xs font-medium ${status.bgColor} ${status.textColor}`}
                          >
                            {status.label}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-sm text-right font-medium text-gray-900 dark:text-white">
                          {formatCurrency(peca.valorVenda)}
                        </td>
                        <td className="px-6 py-4">
                          {isEditing ? (
                            <div className="min-w-[200px]">
                              <LocalArmazenamentoSelect
                                value={selectedLocalId}
                                onChange={setSelectedLocalId}
                                placeholder="Selecione o local"
                                allowEmpty={false}
                              />
                            </div>
                          ) : (
                            <span className="text-sm text-orange-600 dark:text-orange-400 font-medium">
                              Não definido
                            </span>
                          )}
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2 justify-end">
                            {isEditing ? (
                              <>
                                <button
                                  onClick={() => handleAssignLocation(peca.id)}
                                  disabled={!selectedLocalId || definirLocalizacao.isPending}
                                  className="inline-flex items-center gap-1 rounded-lg bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                  <MapPin className="h-3 w-3" />
                                  Salvar
                                </button>
                                <button
                                  onClick={() => {
                                    setSelectedPecaId(null);
                                    setSelectedLocalId('');
                                  }}
                                  className="rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-1.5 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                                >
                                  Cancelar
                                </button>
                              </>
                            ) : (
                              <>
                                <button
                                  onClick={() => setSelectedPecaId(peca.id)}
                                  className="inline-flex items-center gap-1 rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-1.5 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                                >
                                  <MapPin className="h-3 w-3" />
                                  Definir Local
                                </button>
                                <Link
                                  to={`/estoque/${peca.id}`}
                                  className="inline-flex items-center gap-1 text-sm text-blue-600 dark:text-blue-400 hover:underline"
                                >
                                  <ExternalLink className="h-3 w-3" />
                                </Link>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </>
        )}

        {/* Pagination Info */}
        {data && data.totalElements > 0 && (
          <div className="border-t border-gray-200 dark:border-gray-700 p-4 text-sm text-gray-600 dark:text-gray-400">
            Mostrando {data.numberOfElements} de {data.totalElements} peças
          </div>
        )}
      </div>

      {/* Help Text */}
      <div className="mt-6 rounded-lg border border-blue-200 dark:border-blue-800 bg-blue-50 dark:bg-blue-900/20 p-4">
        <h3 className="font-semibold text-blue-900 dark:text-blue-200 mb-2">Dica</h3>
        <p className="text-sm text-blue-800 dark:text-blue-300">
          Você também pode definir o local de armazenamento ao editar uma peça individualmente
          ou ao criar uma nova peça.
        </p>
      </div>
    </div>
  );
};
