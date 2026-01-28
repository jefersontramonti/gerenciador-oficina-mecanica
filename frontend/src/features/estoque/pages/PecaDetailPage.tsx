/**
 * Página de detalhes da Peça com histórico de movimentações
 */

import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  Edit,
  Power,
  PowerOff,
  ArrowDownCircle,
  ArrowUpCircle,
  Settings,
  AlertTriangle,
} from 'lucide-react';
import { formatCurrency } from '@/shared/utils/formatters';
import { usePeca, useDesativarPeca, useReativarPeca } from '../hooks/usePecas';
import { useHistoricoPeca } from '../hooks/useMovimentacoes';
import {
  MovimentacaoList,
  MovimentacaoModal,
} from '../components';
import { UnidadeMedidaLabel, CategoriaPecaLabel, getMargemLucroStatus, getStockStatus } from '../types';
import { AnexosSection } from '@/features/anexos/components';

export const PecaDetailPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();

  const { data: peca, isLoading, error, refetch } = usePeca(id);
  const { data: movimentacoesData, isLoading: isLoadingMovimentacoes } =
    useHistoricoPeca(id);
  const desativarPeca = useDesativarPeca();
  const reativarPeca = useReativarPeca();

  const [movimentacaoModal, setMovimentacaoModal] = useState<{
    isOpen: boolean;
    tipo: 'ENTRADA' | 'SAIDA' | 'AJUSTE';
  }>({
    isOpen: false,
    tipo: 'ENTRADA',
  });

  const handleDesativar = async () => {
    if (id && confirm('Deseja realmente desativar esta peca?')) {
      await desativarPeca.mutateAsync(id);
    }
  };

  const handleReativar = async () => {
    if (id) {
      await reativarPeca.mutateAsync(id);
    }
  };

  const handleMovimentar = (tipo: 'ENTRADA' | 'SAIDA' | 'AJUSTE') => {
    setMovimentacaoModal({ isOpen: true, tipo });
  };

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Carregando...</div>
      </div>
    );
  }

  if (error || !peca) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400 dark:text-red-300">
          Peca nao encontrada
        </div>
      </div>
    );
  }

  const margemStatus = getMargemLucroStatus(peca.margemLucro);
  const stockStatus = getStockStatus(peca.quantidadeAtual, peca.quantidadeMinima);

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3 sm:gap-4">
          <button
            onClick={() => navigate('/estoque')}
            className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-700 shrink-0"
          >
            <ArrowLeft className="h-5 w-5 text-gray-900 dark:text-gray-100" />
          </button>
          <div className="min-w-0">
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100 truncate">
              {peca.nome || peca.codigo}
            </h1>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              <span className="font-mono">{peca.codigo}</span>
              {peca.categoria && (
                <span className="ml-2 inline-flex items-center rounded-full bg-blue-100 dark:bg-blue-900/30 px-2 py-0.5 text-xs font-medium text-blue-800 dark:text-blue-400">
                  {CategoriaPecaLabel[peca.categoria]}
                </span>
              )}
            </p>
          </div>
        </div>

        <div className="flex gap-2 self-end sm:self-auto">
          <button
            onClick={() => navigate(`/estoque/${id}/editar`)}
            className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 sm:px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
          >
            <Edit className="h-5 w-5" />
            <span className="hidden sm:inline">Editar</span>
          </button>
          {peca.ativo ? (
            <button
              onClick={handleDesativar}
              disabled={desativarPeca.isPending}
              className="flex items-center gap-2 rounded-lg border border-red-600 dark:border-red-700 bg-white dark:bg-gray-700 px-3 sm:px-4 py-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <PowerOff className="h-5 w-5" />
              <span className="hidden sm:inline">Desativar</span>
            </button>
          ) : (
            <button
              onClick={handleReativar}
              disabled={reativarPeca.isPending}
              className="flex items-center gap-2 rounded-lg border border-green-600 dark:border-green-700 bg-white dark:bg-gray-700 px-3 sm:px-4 py-2 text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/30 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Power className="h-5 w-5" />
              <span className="hidden sm:inline">Reativar</span>
            </button>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Main Info */}
        <div className="lg:col-span-2 space-y-6">
          {/* Estoque */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Estoque</h2>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Status</label>
                <div className="mt-2">
                  <span
                    className={`inline-flex rounded-full px-3 py-1 text-sm font-semibold ${stockStatus.bgColor} ${stockStatus.textColor}`}
                  >
                    {stockStatus.label}
                  </span>
                </div>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Quantidade Atual</label>
                <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">{peca.quantidadeAtual}</p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  Min: {peca.quantidadeMinima}
                  {peca.quantidadeMaxima != null && ` / Max: ${peca.quantidadeMaxima}`}
                </p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Valor em Estoque</label>
                <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-gray-100">
                  {formatCurrency(peca.valorTotalEstoque)}
                </p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  Custo unit: {formatCurrency(peca.valorCusto)}
                </p>
              </div>
            </div>

            {peca.pontoPedido != null && (
              <div className={`mt-4 rounded-lg border p-3 ${
                peca.atingiuPontoPedido
                  ? 'border-orange-300 dark:border-orange-700 bg-orange-50 dark:bg-orange-900/20'
                  : 'border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-700/50'
              }`}>
                <div className="flex items-center gap-2">
                  {peca.atingiuPontoPedido && (
                    <AlertTriangle className="h-5 w-5 text-orange-500 dark:text-orange-400 shrink-0" />
                  )}
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    <span className="font-medium text-gray-900 dark:text-gray-100">Ponto de Pedido:</span> {peca.pontoPedido} unidades
                    {peca.atingiuPontoPedido && (
                      <span className="ml-2 text-orange-600 dark:text-orange-400 font-semibold">
                        — Reabastecer!
                      </span>
                    )}
                  </p>
                </div>
              </div>
            )}
          </div>

          {/* Informações da Peça */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Informacoes da Peca</h2>

            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Codigo (SKU)</label>
                  <p className="mt-1 font-mono text-gray-900 dark:text-gray-100">{peca.codigo}</p>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Unidade de Medida</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100">
                    {UnidadeMedidaLabel[peca.unidadeMedida]}
                  </p>
                </div>
              </div>

              {peca.descricao && (
                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Descricao Tecnica</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100">{peca.descricao}</p>
                </div>
              )}

              {peca.marca && (
                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Marca</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100">{peca.marca}</p>
                </div>
              )}

              {(peca.codigoOriginal || peca.codigoFabricante) && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {peca.codigoOriginal && (
                    <div>
                      <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Codigo Original</label>
                      <p className="mt-1 font-mono text-gray-900 dark:text-gray-100">{peca.codigoOriginal}</p>
                    </div>
                  )}
                  {peca.codigoFabricante && (
                    <div>
                      <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Codigo do Fabricante</label>
                      <p className="mt-1 font-mono text-gray-900 dark:text-gray-100">{peca.codigoFabricante}</p>
                    </div>
                  )}
                </div>
              )}

              {(peca.codigoBarras || peca.ncm) && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {peca.codigoBarras && (
                    <div>
                      <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Codigo de Barras</label>
                      <p className="mt-1 font-mono text-gray-900 dark:text-gray-100">{peca.codigoBarras}</p>
                    </div>
                  )}
                  {peca.ncm && (
                    <div>
                      <label className="text-sm font-medium text-gray-500 dark:text-gray-400">NCM</label>
                      <p className="mt-1 font-mono text-gray-900 dark:text-gray-100">{peca.ncm}</p>
                    </div>
                  )}
                </div>
              )}

              {peca.localArmazenamento && (
                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Local de Armazenamento</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100">
                    {peca.localArmazenamento.codigo} - {peca.localArmazenamento.descricao}
                  </p>
                  <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                    {peca.localArmazenamento.caminhoCompleto}
                  </p>
                </div>
              )}

              {peca.aplicacao && (
                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Aplicacao</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100">{peca.aplicacao}</p>
                </div>
              )}

              {peca.fornecedorPrincipal && (
                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Fornecedor Principal</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100">{peca.fornecedorPrincipal}</p>
                </div>
              )}

              {peca.observacoes && (
                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Observacoes</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100 whitespace-pre-line">{peca.observacoes}</p>
                </div>
              )}
            </div>
          </div>

          {/* Precificação */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Precificacao</h2>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Valor de Custo</label>
                <p className="mt-1 text-lg font-bold text-gray-900 dark:text-gray-100">{formatCurrency(peca.valorCusto)}</p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Valor de Venda</label>
                <p className="mt-1 text-lg font-bold text-gray-900 dark:text-gray-100">{formatCurrency(peca.valorVenda)}</p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Margem de Lucro</label>
                <p className="mt-1 text-lg font-bold text-gray-900 dark:text-gray-100">{peca.margemLucro.toFixed(2)}%</p>
                <span
                  className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium mt-1 ${margemStatus.bgColor} ${margemStatus.textColor}`}
                >
                  {margemStatus.label}
                </span>
              </div>
            </div>
          </div>

          {/* Fotos da Peça */}
          {id && (
            <AnexosSection
              entidadeTipo="PECA"
              entidadeId={id}
              title="Fotos da Peca"
            />
          )}

          {/* Ações de Movimentação */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Movimentar Estoque</h2>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4">
              <button
                onClick={() => handleMovimentar('ENTRADA')}
                className="flex flex-col items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-6 text-gray-700 dark:text-gray-300 hover:bg-green-50 dark:hover:bg-green-950/30 hover:border-green-300 dark:hover:border-green-700"
              >
                <ArrowDownCircle className="h-8 w-8 text-green-600 dark:text-green-400" />
                <span className="font-medium text-gray-900 dark:text-gray-100">Registrar Entrada</span>
                <span className="text-xs text-gray-500 dark:text-gray-400">
                  Adicionar itens ao estoque
                </span>
              </button>

              <button
                onClick={() => handleMovimentar('SAIDA')}
                className="flex flex-col items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-6 text-gray-700 dark:text-gray-300 hover:bg-red-50 dark:hover:bg-red-950/30 hover:border-red-300 dark:hover:border-red-700"
              >
                <ArrowUpCircle className="h-8 w-8 text-red-600 dark:text-red-400" />
                <span className="font-medium text-gray-900 dark:text-gray-100">Registrar Saida</span>
                <span className="text-xs text-gray-500 dark:text-gray-400">
                  Remover itens do estoque
                </span>
              </button>

              <button
                onClick={() => handleMovimentar('AJUSTE')}
                className="flex flex-col items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-6 text-gray-700 dark:text-gray-300 hover:bg-yellow-50 dark:hover:bg-yellow-950/30 hover:border-yellow-300 dark:hover:border-yellow-700"
              >
                <Settings className="h-8 w-8 text-yellow-600 dark:text-yellow-400" />
                <span className="font-medium text-gray-900 dark:text-gray-100">Ajustar Inventario</span>
                <span className="text-xs text-gray-500 dark:text-gray-400">
                  Corrigir quantidade em estoque
                </span>
              </button>
            </div>
          </div>

          {/* Histórico de Movimentações */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Historico de Movimentacoes</h2>
            <MovimentacaoList
              movimentacoes={movimentacoesData?.content || []}
              isLoading={isLoadingMovimentacoes}
              showPecaInfo={false}
            />
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Status */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h3 className="mb-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Status</h3>
            {peca.ativo ? (
              <span className="inline-flex rounded-full bg-green-100 dark:bg-green-900/30 px-3 py-1 text-sm font-semibold text-green-800 dark:text-green-400">
                Ativo
              </span>
            ) : (
              <span className="inline-flex rounded-full bg-gray-100 dark:bg-gray-700 px-3 py-1 text-sm font-semibold text-gray-800 dark:text-gray-300">
                Inativo
              </span>
            )}
          </div>

          {/* Metadata */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h3 className="mb-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Informacoes</h3>

            <div className="space-y-3">
              <div>
                <label className="text-xs font-medium text-gray-500 dark:text-gray-400">ID</label>
                <p className="mt-1 text-xs font-mono text-gray-600 dark:text-gray-400">{peca.id}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Modal de Movimentação */}
      <MovimentacaoModal
        isOpen={movimentacaoModal.isOpen}
        onClose={() =>
          setMovimentacaoModal({ isOpen: false, tipo: 'ENTRADA' })
        }
        onSuccess={() => refetch()}
        tipo={movimentacaoModal.tipo}
        peca={{
          id: peca.id,
          codigo: peca.codigo,
          descricao: peca.descricao,
          unidadeMedida: peca.unidadeMedida,
          quantidadeAtual: peca.quantidadeAtual,
        }}
      />
    </div>
  );
};
