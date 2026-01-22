import { useState, useRef } from 'react';
import { Link } from 'react-router-dom';
import {
  Upload,
  FileText,
  Calendar,
  CheckCircle,
  AlertCircle,
  ChevronRight,
  RefreshCw,
  Loader2,
} from 'lucide-react';
import { useExtratos, useImportarExtrato } from '../hooks/useConciliacao';
import {
  getStatusExtratoLabel,
  getStatusExtratoColor,
} from '../types/conciliacao';
import type { ExtratoBancarioDTO } from '../types/conciliacao';

export default function ConciliacaoPage() {
  const [page, setPage] = useState(0);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { data, isLoading, isFetching, isError, refetch } = useExtratos(page, 10);
  const importarMutation = useImportarExtrato();

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validar extensão
    if (!file.name.toLowerCase().endsWith('.ofx')) {
      return;
    }

    await importarMutation.mutateAsync({ arquivo: file });

    // Limpar input
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('pt-BR');
  };

  const formatCurrency = (value: number | undefined) => {
    if (value === undefined || value === null) return '-';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="text-center py-12">
        <AlertCircle className="h-12 w-12 text-red-500 mx-auto mb-4" />
        <p className="text-gray-600 dark:text-gray-400">
          Erro ao carregar extratos
        </p>
        <button
          onClick={() => refetch()}
          className="mt-4 text-primary-600 hover:text-primary-700"
        >
          Tentar novamente
        </button>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Conciliação Bancária
          </h1>
          <p className="text-gray-600 dark:text-gray-400 mt-1">
            Importe extratos OFX e concilie com pagamentos do sistema
          </p>
        </div>

        <div className="flex gap-3">
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${isFetching ? 'animate-spin' : ''}`} />
          </button>

          <label className="cursor-pointer">
            <input
              ref={fileInputRef}
              type="file"
              accept=".ofx"
              onChange={handleFileSelect}
              className="hidden"
              disabled={importarMutation.isPending}
            />
            <span className="inline-flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors">
              {importarMutation.isPending ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Importando...
                </>
              ) : (
                <>
                  <Upload className="h-4 w-4" />
                  Importar OFX
                </>
              )}
            </span>
          </label>
        </div>
      </div>

      {/* Empty State */}
      {data?.content.length === 0 && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-12 text-center">
          <FileText className="h-16 w-16 text-gray-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
            Nenhum extrato importado
          </h3>
          <p className="text-gray-600 dark:text-gray-400 mb-6">
            Importe um arquivo OFX do seu banco para começar a conciliação
          </p>
          <label className="cursor-pointer inline-flex items-center gap-2 px-6 py-3 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors">
            <input
              type="file"
              accept=".ofx"
              onChange={handleFileSelect}
              className="hidden"
              disabled={importarMutation.isPending}
            />
            <Upload className="h-5 w-5" />
            Importar Extrato OFX
          </label>
        </div>
      )}

      {/* Lista de Extratos */}
      {data && data.content.length > 0 && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
          <div className="divide-y divide-gray-200 dark:divide-gray-700">
            {data.content.map((extrato: ExtratoBancarioDTO) => (
              <Link
                key={extrato.id}
                to={`/financeiro/conciliacao/${extrato.id}`}
                className="block hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
              >
                <div className="p-4">
                  {/* Mobile Layout */}
                  <div className="lg:hidden space-y-3">
                    {/* Header: Nome e Status */}
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-center gap-2 min-w-0 flex-1">
                        <FileText className="h-4 w-4 text-gray-400 flex-shrink-0" />
                        <h3 className="text-sm font-medium text-gray-900 dark:text-white truncate">
                          {extrato.arquivoNome}
                        </h3>
                      </div>
                      <span
                        className={`px-2 py-0.5 rounded-full text-xs font-medium flex-shrink-0 ${getStatusExtratoColor(
                          extrato.status
                        )}`}
                      >
                        {getStatusExtratoLabel(extrato.status)}
                      </span>
                    </div>

                    {/* Info Grid */}
                    <div className="grid grid-cols-2 gap-2 text-xs text-gray-600 dark:text-gray-400">
                      <span className="flex items-center gap-1">
                        <Calendar className="h-3 w-3" />
                        {formatDate(extrato.dataInicio)} - {formatDate(extrato.dataFim)}
                      </span>
                      <span className="flex items-center gap-1">
                        <FileText className="h-3 w-3" />
                        {extrato.totalTransacoes} transações
                      </span>
                    </div>

                    {/* Progress Bar Mobile */}
                    <div className="space-y-1">
                      <div className="flex justify-between text-xs text-gray-600 dark:text-gray-400">
                        <span>Conciliadas</span>
                        <span>{extrato.totalConciliadas}/{extrato.totalTransacoes}</span>
                      </div>
                      <div className="h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                        <div
                          className="h-full bg-green-500 rounded-full transition-all"
                          style={{ width: `${extrato.percentualConciliado}%` }}
                        />
                      </div>
                    </div>

                    {/* Footer: Status e Saldo */}
                    <div className="flex items-center justify-between pt-2 border-t border-gray-100 dark:border-gray-700">
                      {extrato.totalPendentes > 0 ? (
                        <span className="flex items-center gap-1 text-yellow-600 dark:text-yellow-400 text-xs">
                          <AlertCircle className="h-3 w-3" />
                          {extrato.totalPendentes} pendentes
                        </span>
                      ) : (
                        <span className="flex items-center gap-1 text-green-600 dark:text-green-400 text-xs">
                          <CheckCircle className="h-3 w-3" />
                          Concluído
                        </span>
                      )}
                      {extrato.saldoFinal !== undefined && (
                        <span className="text-xs font-medium text-gray-900 dark:text-white">
                          Saldo: {formatCurrency(extrato.saldoFinal)}
                        </span>
                      )}
                      <ChevronRight className="h-4 w-4 text-gray-400" />
                    </div>
                  </div>

                  {/* Desktop Layout */}
                  <div className="hidden lg:block">
                    <div className="flex items-center justify-between">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-3 mb-2">
                          <FileText className="h-5 w-5 text-gray-400 flex-shrink-0" />
                          <h3 className="text-lg font-medium text-gray-900 dark:text-white truncate">
                            {extrato.arquivoNome}
                          </h3>
                          <span
                            className={`px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusExtratoColor(
                              extrato.status
                            )}`}
                          >
                            {getStatusExtratoLabel(extrato.status)}
                          </span>
                        </div>

                        <div className="flex flex-wrap gap-x-6 gap-y-2 text-sm text-gray-600 dark:text-gray-400">
                          <span className="flex items-center gap-1">
                            <Calendar className="h-4 w-4" />
                            {formatDate(extrato.dataInicio)} a{' '}
                            {formatDate(extrato.dataFim)}
                          </span>
                          <span className="flex items-center gap-1">
                            <FileText className="h-4 w-4" />
                            {extrato.totalTransacoes} transações
                          </span>
                          {extrato.saldoFinal !== undefined && (
                            <span>Saldo: {formatCurrency(extrato.saldoFinal)}</span>
                          )}
                        </div>
                      </div>

                      <div className="flex items-center gap-4 ml-4">
                        {/* Progress */}
                        <div className="w-32">
                          <div className="flex justify-between text-xs text-gray-600 dark:text-gray-400 mb-1">
                            <span>Conciliadas</span>
                            <span>
                              {extrato.totalConciliadas}/{extrato.totalTransacoes}
                            </span>
                          </div>
                          <div className="h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                            <div
                              className="h-full bg-green-500 rounded-full transition-all"
                              style={{
                                width: `${extrato.percentualConciliado}%`,
                              }}
                            />
                          </div>
                        </div>

                        {extrato.totalPendentes > 0 ? (
                          <span className="flex items-center gap-1 text-yellow-600 dark:text-yellow-400 text-sm">
                            <AlertCircle className="h-4 w-4" />
                            {extrato.totalPendentes} pendentes
                          </span>
                        ) : (
                          <span className="flex items-center gap-1 text-green-600 dark:text-green-400 text-sm">
                            <CheckCircle className="h-4 w-4" />
                            Concluído
                          </span>
                        )}

                        <ChevronRight className="h-5 w-5 text-gray-400" />
                      </div>
                    </div>
                  </div>
                </div>
              </Link>
            ))}
          </div>

          {/* Pagination */}
          {data.totalPages > 1 && (
            <div className="px-4 py-3 border-t border-gray-200 dark:border-gray-700 flex items-center justify-between">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Página {data.number + 1} de {data.totalPages}
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={data.first}
                  className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  Anterior
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={data.last}
                  className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  Próxima
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Info Card */}
      <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
        <h4 className="font-medium text-blue-800 dark:text-blue-300 mb-2">
          Como funciona a conciliação
        </h4>
        <ul className="text-sm text-blue-700 dark:text-blue-400 space-y-1">
          <li>
            1. Exporte o extrato do seu banco no formato OFX
          </li>
          <li>
            2. Importe o arquivo aqui e o sistema identificará as transações
          </li>
          <li>
            3. Transações de crédito são automaticamente associadas a pagamentos
          </li>
          <li>
            4. Revise e confirme as sugestões ou faça ajustes manualmente
          </li>
        </ul>
      </div>
    </div>
  );
}
