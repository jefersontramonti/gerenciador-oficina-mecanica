/**
 * Página de Alertas de Estoque (Baixo e Zerado)
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, AlertTriangle, XCircle, ArrowDownCircle } from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/components/ui/tabs';
import { formatCurrency } from '@/shared/utils/formatters';
import { useAlertasEstoqueBaixo, useAlertasEstoqueZerado } from '../hooks/usePecas';
import { StockBadge, UnidadeMedidaBadge, MovimentacaoModal } from '../components';
import type { Peca } from '../types';

export const AlertasEstoquePage = () => {
  const navigate = useNavigate();

  const [pageBaixo, setPageBaixo] = useState(0);
  const [pageZerado, setPageZerado] = useState(0);

  const { data: dataBaixo, isLoading: isLoadingBaixo } = useAlertasEstoqueBaixo(
    pageBaixo,
    20
  );
  const { data: dataZerado, isLoading: isLoadingZerado } = useAlertasEstoqueZerado(
    pageZerado,
    20
  );

  const [movimentacaoModal, setMovimentacaoModal] = useState<{
    isOpen: boolean;
    peca: Peca | null;
  }>({
    isOpen: false,
    peca: null,
  });

  const handleRegistrarEntrada = (peca: Peca) => {
    setMovimentacaoModal({ isOpen: true, peca });
  };

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-6">
        <button
          onClick={() => navigate('/estoque')}
          className="mb-4 flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200"
        >
          <ArrowLeft className="h-4 w-4" />
          Voltar para Estoque
        </button>
        <div className="flex items-center gap-3">
          <AlertTriangle className="h-6 sm:h-8 w-6 sm:w-8 text-orange-600 dark:text-orange-400 shrink-0" />
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
              Alertas de Estoque
            </h1>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              Peças que requerem atenção imediata
            </p>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="baixo" className="space-y-4">
        <TabsList className="grid w-full max-w-md grid-cols-2 bg-gray-100 dark:bg-gray-700">
          <TabsTrigger value="baixo" className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4" />
            Estoque Baixo
            {dataBaixo && (
              <span className="ml-1 rounded-full bg-orange-100 dark:bg-orange-900/40 px-2 py-0.5 text-xs font-medium text-orange-700 dark:text-orange-300">
                {dataBaixo.totalElements}
              </span>
            )}
          </TabsTrigger>
          <TabsTrigger value="zerado" className="flex items-center gap-2">
            <XCircle className="h-4 w-4" />
            Estoque Zerado
            {dataZerado && (
              <span className="ml-1 rounded-full bg-red-100 dark:bg-red-900/40 px-2 py-0.5 text-xs font-medium text-red-700 dark:text-red-300">
                {dataZerado.totalElements}
              </span>
            )}
          </TabsTrigger>
        </TabsList>

        {/* Tab: Estoque Baixo */}
        <TabsContent value="baixo" className="space-y-4">
          <div className="rounded-lg border border-orange-200 dark:border-orange-800 bg-orange-50 dark:bg-orange-900/20 p-4">
            <p className="text-sm text-orange-800 dark:text-orange-300">
              <strong>Atenção:</strong> As peças abaixo estão com quantidade atual{' '}
              <strong>igual ou inferior</strong> à quantidade mínima configurada.
            </p>
          </div>

          {/* Mobile: Card Layout */}
          <div className="space-y-3 lg:hidden">
            {isLoadingBaixo ? (
              <div className="flex justify-center items-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              </div>
            ) : !dataBaixo || dataBaixo.content.length === 0 ? (
              <div className="rounded-lg bg-white dark:bg-gray-800 p-8 text-center text-gray-500 dark:text-gray-400 shadow">
                Nenhuma peça com estoque baixo
              </div>
            ) : (
              dataBaixo.content.map((peca) => (
                <div key={peca.id} className="rounded-lg bg-white dark:bg-gray-800 p-4 shadow">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-mono text-sm font-medium text-gray-900 dark:text-white">{peca.codigo}</span>
                        <StockBadge quantidadeAtual={peca.quantidadeAtual} quantidadeMinima={peca.quantidadeMinima} />
                      </div>
                      <p className="mt-1 text-sm text-gray-700 dark:text-gray-300 truncate">{peca.descricao}</p>
                    </div>
                    <button
                      onClick={() => handleRegistrarEntrada(peca)}
                      className="inline-flex items-center gap-1 rounded-lg border border-green-600 dark:border-green-500 px-3 py-1.5 text-sm font-medium text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/30 shrink-0"
                    >
                      <ArrowDownCircle className="h-4 w-4" />
                      <span className="hidden sm:inline">Entrada</span>
                    </button>
                  </div>
                  <div className="mt-3 grid grid-cols-3 gap-2 text-sm">
                    <div>
                      <span className="text-xs text-gray-500 dark:text-gray-400">Atual</span>
                      <p className="font-medium text-orange-600 dark:text-orange-400">{peca.quantidadeAtual}</p>
                    </div>
                    <div>
                      <span className="text-xs text-gray-500 dark:text-gray-400">Mínimo</span>
                      <p className="font-medium text-gray-900 dark:text-white">{peca.quantidadeMinima}</p>
                    </div>
                    <div>
                      <span className="text-xs text-gray-500 dark:text-gray-400">Custo</span>
                      <p className="font-medium text-gray-900 dark:text-white">{formatCurrency(peca.valorCusto)}</p>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Desktop: Table Layout */}
          <div className="hidden lg:block rounded-lg bg-white dark:bg-gray-800 shadow">
            {isLoadingBaixo ? (
              <div className="flex justify-center items-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              </div>
            ) : !dataBaixo || dataBaixo.content.length === 0 ? (
              <div className="text-center py-12 text-gray-500 dark:text-gray-400">
                Nenhuma peça com estoque baixo
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full divide-y divide-gray-200 dark:divide-gray-700">
                  <thead className="bg-gray-50 dark:bg-gray-700">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Código
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Descrição
                      </th>
                      <th className="px-6 py-3 text-center text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Unidade
                      </th>
                      <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Qtd Atual
                      </th>
                      <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Qtd Mínima
                      </th>
                      <th className="px-6 py-3 text-center text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Status
                      </th>
                      <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Valor Unit.
                      </th>
                      <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Ações
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-800">
                    {dataBaixo.content.map((peca) => (
                      <tr key={peca.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                        <td className="px-6 py-4 text-sm font-medium text-gray-900 dark:text-white">
                          {peca.codigo}
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-900 dark:text-white max-w-xs truncate">
                          {peca.descricao}
                        </td>
                        <td className="px-6 py-4 text-center">
                          <UnidadeMedidaBadge unidade={peca.unidadeMedida} />
                        </td>
                        <td className="px-6 py-4 text-sm text-right font-medium text-orange-600 dark:text-orange-400">
                          {peca.quantidadeAtual}
                        </td>
                        <td className="px-6 py-4 text-sm text-right text-gray-900 dark:text-white">
                          {peca.quantidadeMinima}
                        </td>
                        <td className="px-6 py-4 text-center">
                          <StockBadge
                            quantidadeAtual={peca.quantidadeAtual}
                            quantidadeMinima={peca.quantidadeMinima}
                          />
                        </td>
                        <td className="px-6 py-4 text-sm text-right text-gray-900 dark:text-white">
                          {formatCurrency(peca.valorCusto)}
                        </td>
                        <td className="px-6 py-4 text-right">
                          <button
                            onClick={() => handleRegistrarEntrada(peca)}
                            className="inline-flex items-center gap-1 rounded-lg border border-green-600 dark:border-green-500 px-3 py-1.5 text-sm font-medium text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/30"
                          >
                            <ArrowDownCircle className="h-4 w-4" />
                            Entrada
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Paginação */}
          {dataBaixo && dataBaixo.totalPages > 1 && (
            <div className="flex flex-col gap-3 sm:flex-row sm:justify-between sm:items-center">
              <p className="text-sm text-gray-600 dark:text-gray-400 text-center sm:text-left">
                Mostrando {dataBaixo.content.length} de {dataBaixo.totalElements}{' '}
                peças
              </p>
              <div className="flex gap-2 justify-center sm:justify-end">
                <button
                  disabled={dataBaixo.first}
                  onClick={() => setPageBaixo((prev) => prev - 1)}
                  className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Anterior
                </button>
                <button
                  disabled={dataBaixo.last}
                  onClick={() => setPageBaixo((prev) => prev + 1)}
                  className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Próxima
                </button>
              </div>
            </div>
          )}
        </TabsContent>

        {/* Tab: Estoque Zerado */}
        <TabsContent value="zerado" className="space-y-4">
          <div className="rounded-lg border border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/20 p-4">
            <p className="text-sm text-red-800 dark:text-red-300">
              <strong>Urgente:</strong> As peças abaixo estão com{' '}
              <strong>quantidade zerada</strong>. Registre entradas imediatamente para
              evitar problemas operacionais.
            </p>
          </div>

          {/* Mobile: Card Layout */}
          <div className="space-y-3 lg:hidden">
            {isLoadingZerado ? (
              <div className="flex justify-center items-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              </div>
            ) : !dataZerado || dataZerado.content.length === 0 ? (
              <div className="rounded-lg bg-white dark:bg-gray-800 p-8 text-center text-gray-500 dark:text-gray-400 shadow">
                Nenhuma peça com estoque zerado
              </div>
            ) : (
              dataZerado.content.map((peca) => (
                <div key={peca.id} className="rounded-lg bg-red-50/50 dark:bg-red-950/20 border border-red-200 dark:border-red-800 p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-mono text-sm font-medium text-gray-900 dark:text-white">{peca.codigo}</span>
                        <StockBadge quantidadeAtual={peca.quantidadeAtual} quantidadeMinima={peca.quantidadeMinima} />
                      </div>
                      <p className="mt-1 text-sm text-gray-700 dark:text-gray-300 truncate">{peca.descricao}</p>
                    </div>
                    <button
                      onClick={() => handleRegistrarEntrada(peca)}
                      className="inline-flex items-center gap-1 rounded-lg bg-green-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-green-700 dark:bg-green-700 dark:hover:bg-green-600 shrink-0"
                    >
                      <ArrowDownCircle className="h-4 w-4" />
                      <span className="hidden sm:inline">Urgente</span>
                    </button>
                  </div>
                  <div className="mt-3 grid grid-cols-3 gap-2 text-sm">
                    <div>
                      <span className="text-xs text-gray-500 dark:text-gray-400">Atual</span>
                      <p className="font-medium text-red-600 dark:text-red-400">0</p>
                    </div>
                    <div>
                      <span className="text-xs text-gray-500 dark:text-gray-400">Mínimo</span>
                      <p className="font-medium text-gray-900 dark:text-white">{peca.quantidadeMinima}</p>
                    </div>
                    <div>
                      <span className="text-xs text-gray-500 dark:text-gray-400">Custo</span>
                      <p className="font-medium text-gray-900 dark:text-white">{formatCurrency(peca.valorCusto)}</p>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Desktop: Table Layout */}
          <div className="hidden lg:block rounded-lg bg-white dark:bg-gray-800 shadow">
            {isLoadingZerado ? (
              <div className="flex justify-center items-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              </div>
            ) : !dataZerado || dataZerado.content.length === 0 ? (
              <div className="text-center py-12 text-gray-500 dark:text-gray-400">
                Nenhuma peça com estoque zerado
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full divide-y divide-gray-200 dark:divide-gray-700">
                  <thead className="bg-gray-50 dark:bg-gray-700">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Código
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Descrição
                      </th>
                      <th className="px-6 py-3 text-center text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Unidade
                      </th>
                      <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Qtd Mínima
                      </th>
                      <th className="px-6 py-3 text-center text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Status
                      </th>
                      <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Valor Unit.
                      </th>
                      <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                        Ações
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-800">
                    {dataZerado.content.map((peca) => (
                      <tr key={peca.id} className="bg-red-50/50 dark:bg-red-950/20 hover:bg-red-50 dark:hover:bg-red-950/30">
                        <td className="px-6 py-4 text-sm font-medium text-gray-900 dark:text-white">
                          {peca.codigo}
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-900 dark:text-white max-w-xs truncate">
                          {peca.descricao}
                        </td>
                        <td className="px-6 py-4 text-center">
                          <UnidadeMedidaBadge unidade={peca.unidadeMedida} />
                        </td>
                        <td className="px-6 py-4 text-sm text-right text-gray-900 dark:text-white">
                          {peca.quantidadeMinima}
                        </td>
                        <td className="px-6 py-4 text-center">
                          <StockBadge
                            quantidadeAtual={peca.quantidadeAtual}
                            quantidadeMinima={peca.quantidadeMinima}
                          />
                        </td>
                        <td className="px-6 py-4 text-sm text-right text-gray-900 dark:text-white">
                          {formatCurrency(peca.valorCusto)}
                        </td>
                        <td className="px-6 py-4 text-right">
                          <button
                            onClick={() => handleRegistrarEntrada(peca)}
                            className="inline-flex items-center gap-1 rounded-lg bg-green-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-green-700 dark:bg-green-700 dark:hover:bg-green-600"
                          >
                            <ArrowDownCircle className="h-4 w-4" />
                            Entrada Urgente
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Paginação */}
          {dataZerado && dataZerado.totalPages > 1 && (
            <div className="flex flex-col gap-3 sm:flex-row sm:justify-between sm:items-center">
              <p className="text-sm text-gray-600 dark:text-gray-400 text-center sm:text-left">
                Mostrando {dataZerado.content.length} de {dataZerado.totalElements}{' '}
                peças
              </p>
              <div className="flex gap-2 justify-center sm:justify-end">
                <button
                  disabled={dataZerado.first}
                  onClick={() => setPageZerado((prev) => prev - 1)}
                  className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Anterior
                </button>
                <button
                  disabled={dataZerado.last}
                  onClick={() => setPageZerado((prev) => prev + 1)}
                  className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Próxima
                </button>
              </div>
            </div>
          )}
        </TabsContent>
      </Tabs>

      {/* Modal de Entrada */}
      {movimentacaoModal.peca && (
        <MovimentacaoModal
          isOpen={movimentacaoModal.isOpen}
          onClose={() => setMovimentacaoModal({ isOpen: false, peca: null })}
          tipo="ENTRADA"
          peca={{
            id: movimentacaoModal.peca.id,
            codigo: movimentacaoModal.peca.codigo,
            descricao: movimentacaoModal.peca.descricao,
            unidadeMedida: movimentacaoModal.peca.unidadeMedida,
            quantidadeAtual: movimentacaoModal.peca.quantidadeAtual,
          }}
        />
      )}
    </div>
  );
};
