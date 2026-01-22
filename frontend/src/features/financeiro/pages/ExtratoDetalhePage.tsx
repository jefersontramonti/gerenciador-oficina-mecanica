import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Calendar,
  FileText,
  CheckCircle,
  XCircle,
  RefreshCw,
  AlertCircle,
  ChevronDown,
  ChevronUp,
  Link as LinkIcon,
  Loader2,
} from 'lucide-react';
import {
  useExtrato,
  useTransacoesComSugestoes,
  useConciliarTransacao,
  useIgnorarTransacao,
  useDesconciliarTransacao,
} from '../hooks/useConciliacao';
import {
  getStatusConciliacaoLabel,
  getStatusConciliacaoColor,
  getTipoTransacaoColor,
} from '../types/conciliacao';
import type {
  TransacaoExtratoDTO,
  SugestaoConciliacaoDTO,
} from '../types/conciliacao';

export default function ExtratoDetalhePage() {
  const { id } = useParams<{ id: string }>();
  const [expandedTransaction, setExpandedTransaction] = useState<string | null>(
    null
  );
  const [filterStatus, setFilterStatus] = useState<string>('all');

  const { data: extrato, isLoading: loadingExtrato } = useExtrato(id || '');
  const {
    data: transacoes,
    isLoading: loadingTransacoes,
    refetch,
  } = useTransacoesComSugestoes(id || '');

  const conciliarMutation = useConciliarTransacao();
  const ignorarMutation = useIgnorarTransacao();
  const desconciliarMutation = useDesconciliarTransacao();

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('pt-BR');
  };

  const handleConciliar = async (
    transacaoId: string,
    pagamentoId: string
  ) => {
    if (!id) return;
    await conciliarMutation.mutateAsync({
      transacaoId,
      pagamentoId,
      extratoId: id,
    });
    setExpandedTransaction(null);
  };

  const handleIgnorar = async (transacaoId: string) => {
    if (!id) return;
    await ignorarMutation.mutateAsync({
      transacaoId,
      extratoId: id,
    });
  };

  const handleDesconciliar = async (transacaoId: string) => {
    if (!id) return;
    await desconciliarMutation.mutateAsync({
      transacaoId,
      extratoId: id,
    });
  };

  const filteredTransacoes = transacoes?.filter((t) => {
    if (filterStatus === 'all') return true;
    return t.status === filterStatus;
  });

  if (loadingExtrato || loadingTransacoes) {
    return (
      <div className="flex justify-center items-center h-64">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (!extrato) {
    return (
      <div className="text-center py-12">
        <AlertCircle className="h-12 w-12 text-red-500 mx-auto mb-4" />
        <p className="text-gray-600 dark:text-gray-400">Extrato não encontrado</p>
        <Link
          to="/financeiro/conciliacao"
          className="mt-4 text-primary-600 hover:text-primary-700"
        >
          Voltar para lista
        </Link>
      </div>
    );
  }

  const pendentes = transacoes?.filter((t) => t.status === 'NAO_CONCILIADA').length || 0;
  const conciliadas = transacoes?.filter((t) => t.status === 'CONCILIADA').length || 0;
  const ignoradas = transacoes?.filter((t) => t.status === 'IGNORADA').length || 0;

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-4">
          <Link
            to="/financeiro/conciliacao"
            className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-xl font-bold text-gray-900 dark:text-white">
              {extrato.arquivoNome}
            </h1>
            <p className="text-sm text-gray-600 dark:text-gray-400 flex items-center gap-2">
              <Calendar className="h-4 w-4" />
              {formatDate(extrato.dataInicio)} a {formatDate(extrato.dataFim)}
            </p>
          </div>
        </div>

        <button
          onClick={() => refetch()}
          className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors flex items-center gap-2"
        >
          <RefreshCw className="h-4 w-4" />
          Atualizar
        </button>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-4">
          <div className="flex items-center gap-2 text-gray-600 dark:text-gray-400 mb-1">
            <FileText className="h-4 w-4" />
            <span className="text-sm">Total</span>
          </div>
          <p className="text-2xl font-bold text-gray-900 dark:text-white">
            {extrato.totalTransacoes}
          </p>
        </div>

        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-4">
          <div className="flex items-center gap-2 text-green-600 dark:text-green-400 mb-1">
            <CheckCircle className="h-4 w-4" />
            <span className="text-sm">Conciliadas</span>
          </div>
          <p className="text-2xl font-bold text-green-600 dark:text-green-400">
            {conciliadas}
          </p>
        </div>

        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-4">
          <div className="flex items-center gap-2 text-yellow-600 dark:text-yellow-400 mb-1">
            <AlertCircle className="h-4 w-4" />
            <span className="text-sm">Pendentes</span>
          </div>
          <p className="text-2xl font-bold text-yellow-600 dark:text-yellow-400">
            {pendentes}
          </p>
        </div>

        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-4">
          <div className="flex items-center gap-2 text-gray-600 dark:text-gray-400 mb-1">
            <XCircle className="h-4 w-4" />
            <span className="text-sm">Ignoradas</span>
          </div>
          <p className="text-2xl font-bold text-gray-600 dark:text-gray-400">
            {ignoradas}
          </p>
        </div>
      </div>

      {/* Progress Bar */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-4">
        <div className="flex justify-between text-sm text-gray-600 dark:text-gray-400 mb-2">
          <span>Progresso da conciliação</span>
          <span>{extrato.percentualConciliado.toFixed(1)}%</span>
        </div>
        <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
          <div
            className="h-full bg-green-500 rounded-full transition-all duration-500"
            style={{ width: `${extrato.percentualConciliado}%` }}
          />
        </div>
      </div>

      {/* Filter */}
      <div className="flex gap-2">
        <button
          onClick={() => setFilterStatus('all')}
          className={`px-3 py-1.5 rounded-lg text-sm transition-colors ${
            filterStatus === 'all'
              ? 'bg-primary-600 text-white'
              : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
          }`}
        >
          Todas ({transacoes?.length || 0})
        </button>
        <button
          onClick={() => setFilterStatus('NAO_CONCILIADA')}
          className={`px-3 py-1.5 rounded-lg text-sm transition-colors ${
            filterStatus === 'NAO_CONCILIADA'
              ? 'bg-yellow-600 text-white'
              : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
          }`}
        >
          Pendentes ({pendentes})
        </button>
        <button
          onClick={() => setFilterStatus('CONCILIADA')}
          className={`px-3 py-1.5 rounded-lg text-sm transition-colors ${
            filterStatus === 'CONCILIADA'
              ? 'bg-green-600 text-white'
              : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
          }`}
        >
          Conciliadas ({conciliadas})
        </button>
        <button
          onClick={() => setFilterStatus('IGNORADA')}
          className={`px-3 py-1.5 rounded-lg text-sm transition-colors ${
            filterStatus === 'IGNORADA'
              ? 'bg-gray-600 text-white'
              : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
          }`}
        >
          Ignoradas ({ignoradas})
        </button>
      </div>

      {/* Transactions List */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div className="divide-y divide-gray-200 dark:divide-gray-700">
          {filteredTransacoes?.map((transacao) => (
            <TransacaoItem
              key={transacao.id}
              transacao={transacao}
              isExpanded={expandedTransaction === transacao.id}
              onToggle={() =>
                setExpandedTransaction(
                  expandedTransaction === transacao.id ? null : transacao.id
                )
              }
              onConciliar={handleConciliar}
              onIgnorar={handleIgnorar}
              onDesconciliar={handleDesconciliar}
              isPending={
                conciliarMutation.isPending ||
                ignorarMutation.isPending ||
                desconciliarMutation.isPending
              }
            />
          ))}
        </div>

        {filteredTransacoes?.length === 0 && (
          <div className="p-8 text-center text-gray-500 dark:text-gray-400">
            Nenhuma transação encontrada
          </div>
        )}
      </div>
    </div>
  );
}

// Transaction Item Component
function TransacaoItem({
  transacao,
  isExpanded,
  onToggle,
  onConciliar,
  onIgnorar,
  onDesconciliar,
  isPending,
}: {
  transacao: TransacaoExtratoDTO;
  isExpanded: boolean;
  onToggle: () => void;
  onConciliar: (transacaoId: string, pagamentoId: string) => void;
  onIgnorar: (transacaoId: string) => void;
  onDesconciliar: (transacaoId: string) => void;
  isPending: boolean;
}) {
  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('pt-BR');
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  };

  const hasSugestoes =
    transacao.sugestoes && transacao.sugestoes.length > 0;
  const isPendente = transacao.status === 'NAO_CONCILIADA';
  const isCredito = transacao.tipo === 'CREDITO';

  return (
    <div className="divide-y divide-gray-100 dark:divide-gray-700/50">
      <div
        className={`p-4 ${
          isPendente && isCredito && hasSugestoes
            ? 'cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50'
            : ''
        }`}
        onClick={isPendente && isCredito && hasSugestoes ? onToggle : undefined}
      >
        <div className="flex items-center justify-between gap-4">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-3 mb-1">
              <span
                className={`font-mono text-lg font-semibold ${getTipoTransacaoColor(
                  transacao.tipo
                )}`}
              >
                {transacao.tipo === 'CREDITO' ? '+' : '-'}
                {formatCurrency(transacao.valor)}
              </span>
              <span
                className={`px-2 py-0.5 rounded-full text-xs font-medium ${getStatusConciliacaoColor(
                  transacao.status
                )}`}
              >
                {getStatusConciliacaoLabel(transacao.status)}
              </span>
            </div>
            <p className="text-sm text-gray-600 dark:text-gray-400 truncate">
              {transacao.descricao || 'Sem descrição'}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-500 mt-1">
              {formatDate(transacao.dataTransacao)}
              {transacao.identificadorBanco && (
                <span className="ml-2">ID: {transacao.identificadorBanco}</span>
              )}
            </p>
          </div>

          <div className="flex items-center gap-2">
            {/* Actions */}
            {isPendente && (
              <>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onIgnorar(transacao.id);
                  }}
                  disabled={isPending}
                  className="p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                  title="Ignorar"
                >
                  <XCircle className="h-4 w-4" />
                </button>
              </>
            )}

            {transacao.status === 'CONCILIADA' && (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onDesconciliar(transacao.id);
                }}
                disabled={isPending}
                className="p-2 text-gray-400 hover:text-red-600 dark:hover:text-red-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                title="Desfazer conciliação"
              >
                <RefreshCw className="h-4 w-4" />
              </button>
            )}

            {transacao.status === 'IGNORADA' && (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onDesconciliar(transacao.id);
                }}
                disabled={isPending}
                className="p-2 text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                title="Restaurar"
              >
                <RefreshCw className="h-4 w-4" />
              </button>
            )}

            {/* Expand indicator for suggestions */}
            {isPendente && isCredito && hasSugestoes && (
              <span className="text-gray-400">
                {isExpanded ? (
                  <ChevronUp className="h-5 w-5" />
                ) : (
                  <ChevronDown className="h-5 w-5" />
                )}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Suggestions Dropdown */}
      {isExpanded && hasSugestoes && (
        <div className="bg-gray-50 dark:bg-gray-900/50 p-4">
          <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
            Sugestões de conciliação
          </h4>
          <div className="space-y-2">
            {transacao.sugestoes?.map((sugestao: SugestaoConciliacaoDTO) => (
              <div
                key={sugestao.pagamentoId}
                className="flex items-center justify-between bg-white dark:bg-gray-800 p-3 rounded-lg border border-gray-200 dark:border-gray-700"
              >
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-gray-900 dark:text-white">
                      {formatCurrency(sugestao.valor)}
                    </span>
                    <span className="text-xs px-2 py-0.5 bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 rounded">
                      {sugestao.score.toFixed(0)}% match
                    </span>
                  </div>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    {sugestao.clienteNome && (
                      <span className="mr-2">{sugestao.clienteNome}</span>
                    )}
                    {sugestao.osNumero && (
                      <span className="text-gray-500">OS #{sugestao.osNumero}</span>
                    )}
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-500">
                    {formatDate(sugestao.dataPagamento)} - {sugestao.tipoPagamento}
                    {sugestao.motivoSugestao && (
                      <span className="ml-2 italic">({sugestao.motivoSugestao})</span>
                    )}
                  </p>
                </div>
                <button
                  onClick={() => onConciliar(transacao.id, sugestao.pagamentoId)}
                  disabled={isPending}
                  className="flex items-center gap-1 px-3 py-1.5 bg-green-600 text-white text-sm rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50"
                >
                  {isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <LinkIcon className="h-4 w-4" />
                  )}
                  Vincular
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
