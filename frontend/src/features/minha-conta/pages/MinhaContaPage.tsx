/**
 * Página principal "Minha Conta" - Resumo financeiro da oficina
 */

import { Link } from 'react-router-dom';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import {
  CreditCard,
  FileText,
  AlertCircle,
  CheckCircle,
  Clock,
  ArrowRight,
  DollarSign,
  Calendar,
  TrendingUp,
} from 'lucide-react';
import { useMinhaContaResumo } from '../hooks/useMinhaContaFaturas';
import { StatusFatura, StatusFaturaLabels, StatusFaturaCores } from '../types/fatura';

const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const formatDate = (dateString?: string): string => {
  if (!dateString) return '-';
  try {
    return format(new Date(dateString), 'dd/MM/yyyy', { locale: ptBR });
  } catch {
    return '-';
  }
};

export function MinhaContaPage() {
  const { data: resumo, isLoading, error } = useMinhaContaResumo();

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[50vh]">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 dark:border-blue-400 border-t-transparent"></div>
      </div>
    );
  }

  if (error || !resumo) {
    return (
      <div className="p-4 sm:p-6">
        <div className="rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400">
          <p className="font-semibold">Erro ao carregar dados da conta</p>
          <p className="mt-1 text-sm">{error?.message || 'Tente novamente mais tarde.'}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
            Minha Conta
          </h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            Gerencie suas faturas e informações de pagamento
          </p>
        </div>
        <Link
          to="/minha-conta/faturas"
          className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 dark:bg-blue-700 text-white rounded-lg hover:bg-blue-700 dark:hover:bg-blue-600 text-sm font-medium"
        >
          <FileText className="h-4 w-4" />
          Ver Todas as Faturas
        </Link>
      </div>

      {/* Alerta de inadimplência */}
      {!resumo.contaEmDia && (
        <div className="rounded-lg border border-red-300 dark:border-red-800 bg-red-50 dark:bg-red-900/30 p-4">
          <div className="flex items-start gap-3">
            <AlertCircle className="h-5 w-5 text-red-600 dark:text-red-400 shrink-0 mt-0.5" />
            <div>
              <h3 className="font-semibold text-red-800 dark:text-red-300">
                Você possui faturas em atraso
              </h3>
              <p className="mt-1 text-sm text-red-700 dark:text-red-400">
                Regularize seus pagamentos para evitar a suspensão dos serviços.
                Valor total em atraso: {formatCurrency(resumo.valorVencido)}
              </p>
              <Link
                to="/minha-conta/faturas?status=VENCIDO"
                className="mt-2 inline-flex items-center gap-1 text-sm font-medium text-red-700 dark:text-red-300 hover:underline"
              >
                Ver faturas vencidas
                <ArrowRight className="h-4 w-4" />
              </Link>
            </div>
          </div>
        </div>
      )}

      {/* Cards de resumo */}
      <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
        {/* Plano atual */}
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow border border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400 truncate">
                Plano Atual
              </p>
              <p className="mt-1 text-lg sm:text-xl font-bold text-gray-900 dark:text-white truncate">
                {resumo.planoNome || 'Sem plano'}
              </p>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                {formatCurrency(resumo.valorMensalidade)}/mês
              </p>
            </div>
            <div className="rounded-full bg-purple-100 dark:bg-purple-900/30 p-2 sm:p-3 shrink-0">
              <CreditCard className="h-5 w-5 sm:h-6 sm:w-6 text-purple-600 dark:text-purple-400" />
            </div>
          </div>
        </div>

        {/* Faturas pendentes */}
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow border border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400 truncate">
                Pendentes
              </p>
              <p className="mt-1 text-xl sm:text-2xl font-bold text-yellow-600 dark:text-yellow-400">
                {resumo.faturasPendentes}
              </p>
              <p className="text-sm text-gray-500 dark:text-gray-400 truncate">
                {formatCurrency(resumo.valorPendente)}
              </p>
            </div>
            <div className="rounded-full bg-yellow-100 dark:bg-yellow-900/30 p-2 sm:p-3 shrink-0">
              <Clock className="h-5 w-5 sm:h-6 sm:w-6 text-yellow-600 dark:text-yellow-400" />
            </div>
          </div>
        </div>

        {/* Faturas vencidas */}
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow border border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400 truncate">
                Vencidas
              </p>
              <p className="mt-1 text-xl sm:text-2xl font-bold text-red-600 dark:text-red-400">
                {resumo.faturasVencidas}
              </p>
              <p className="text-sm text-gray-500 dark:text-gray-400 truncate">
                {formatCurrency(resumo.valorVencido)}
              </p>
            </div>
            <div className="rounded-full bg-red-100 dark:bg-red-900/30 p-2 sm:p-3 shrink-0">
              <AlertCircle className="h-5 w-5 sm:h-6 sm:w-6 text-red-600 dark:text-red-400" />
            </div>
          </div>
        </div>

        {/* Pago nos últimos 12 meses */}
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow border border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400 truncate">
                Últimos 12 meses
              </p>
              <p className="mt-1 text-lg sm:text-xl font-bold text-green-600 dark:text-green-400 truncate">
                {formatCurrency(resumo.valorPagoUltimos12Meses)}
              </p>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                {resumo.faturasPagas} fatura(s) paga(s)
              </p>
            </div>
            <div className="rounded-full bg-green-100 dark:bg-green-900/30 p-2 sm:p-3 shrink-0">
              <TrendingUp className="h-5 w-5 sm:h-6 sm:w-6 text-green-600 dark:text-green-400" />
            </div>
          </div>
        </div>
      </div>

      {/* Próxima fatura */}
      {resumo.proximaFatura && (
        <div className="rounded-lg bg-white dark:bg-gray-800 shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
          <div className="px-4 sm:px-6 py-4 border-b border-gray-200 dark:border-gray-700">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Próxima Fatura
            </h2>
          </div>
          <div className="p-4 sm:p-6">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
              <div className="space-y-2">
                <div className="flex items-center gap-3">
                  <span className="text-lg font-semibold text-gray-900 dark:text-white">
                    {resumo.proximaFatura.numero}
                  </span>
                  <span
                    className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      StatusFaturaCores[resumo.proximaFatura.status as StatusFatura].bg
                    } ${StatusFaturaCores[resumo.proximaFatura.status as StatusFatura].text}`}
                  >
                    {StatusFaturaLabels[resumo.proximaFatura.status as StatusFatura]}
                  </span>
                </div>
                <div className="flex flex-wrap gap-4 text-sm text-gray-600 dark:text-gray-400">
                  <span className="flex items-center gap-1">
                    <Calendar className="h-4 w-4" />
                    Ref: {resumo.proximaFatura.mesReferenciaFormatado}
                  </span>
                  <span className="flex items-center gap-1">
                    <Clock className="h-4 w-4" />
                    Venc: {formatDate(resumo.proximaFatura.dataVencimento)}
                  </span>
                  <span className="flex items-center gap-1">
                    <DollarSign className="h-4 w-4" />
                    {formatCurrency(resumo.proximaFatura.valorTotal)}
                  </span>
                </div>
                {resumo.proximaFatura.diasAteVencimento !== null && (
                  <p
                    className={`text-sm font-medium ${
                      resumo.proximaFatura.diasAteVencimento < 0
                        ? 'text-red-600 dark:text-red-400'
                        : resumo.proximaFatura.diasAteVencimento <= 3
                        ? 'text-yellow-600 dark:text-yellow-400'
                        : 'text-gray-600 dark:text-gray-400'
                    }`}
                  >
                    {resumo.proximaFatura.diasAteVencimento < 0
                      ? `Vencida há ${Math.abs(resumo.proximaFatura.diasAteVencimento)} dia(s)`
                      : resumo.proximaFatura.diasAteVencimento === 0
                      ? 'Vence hoje'
                      : `Vence em ${resumo.proximaFatura.diasAteVencimento} dia(s)`}
                  </p>
                )}
              </div>
              {resumo.proximaFatura.pagavel && (
                <Link
                  to={`/minha-conta/faturas/${resumo.proximaFatura.id}`}
                  className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-green-600 dark:bg-green-700 text-white rounded-lg hover:bg-green-700 dark:hover:bg-green-600 text-sm font-medium w-full sm:w-auto"
                >
                  <CreditCard className="h-4 w-4" />
                  Pagar Agora
                </Link>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Status da conta */}
      <div className="rounded-lg bg-white dark:bg-gray-800 shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div className="px-4 sm:px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
            Status da Conta
          </h2>
        </div>
        <div className="p-4 sm:p-6">
          <div className="flex items-center gap-3">
            {resumo.contaEmDia ? (
              <>
                <div className="rounded-full bg-green-100 dark:bg-green-900/30 p-2">
                  <CheckCircle className="h-6 w-6 text-green-600 dark:text-green-400" />
                </div>
                <div>
                  <p className="font-semibold text-green-800 dark:text-green-300">
                    Conta em dia
                  </p>
                  <p className="text-sm text-green-700 dark:text-green-400">
                    Todos os pagamentos estão regularizados.
                  </p>
                </div>
              </>
            ) : (
              <>
                <div className="rounded-full bg-red-100 dark:bg-red-900/30 p-2">
                  <AlertCircle className="h-6 w-6 text-red-600 dark:text-red-400" />
                </div>
                <div>
                  <p className="font-semibold text-red-800 dark:text-red-300">
                    Conta inadimplente
                  </p>
                  <p className="text-sm text-red-700 dark:text-red-400">
                    Você possui {resumo.faturasVencidas} fatura(s) vencida(s).
                    Regularize para evitar suspensão.
                  </p>
                </div>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Links rápidos */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Link
          to="/minha-conta/faturas"
          className="flex items-center gap-3 p-4 rounded-lg bg-white dark:bg-gray-800 shadow border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
        >
          <div className="rounded-full bg-blue-100 dark:bg-blue-900/30 p-2">
            <FileText className="h-5 w-5 text-blue-600 dark:text-blue-400" />
          </div>
          <div>
            <p className="font-medium text-gray-900 dark:text-white">
              Minhas Faturas
            </p>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Ver todas as faturas
            </p>
          </div>
          <ArrowRight className="h-5 w-5 text-gray-400 ml-auto" />
        </Link>

        <Link
          to="/minha-conta/pagamentos"
          className="flex items-center gap-3 p-4 rounded-lg bg-white dark:bg-gray-800 shadow border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
        >
          <div className="rounded-full bg-green-100 dark:bg-green-900/30 p-2">
            <DollarSign className="h-5 w-5 text-green-600 dark:text-green-400" />
          </div>
          <div>
            <p className="font-medium text-gray-900 dark:text-white">
              Histórico de Pagamentos
            </p>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Ver pagamentos realizados
            </p>
          </div>
          <ArrowRight className="h-5 w-5 text-gray-400 ml-auto" />
        </Link>

        <Link
          to="/configuracoes"
          className="flex items-center gap-3 p-4 rounded-lg bg-white dark:bg-gray-800 shadow border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
        >
          <div className="rounded-full bg-purple-100 dark:bg-purple-900/30 p-2">
            <CreditCard className="h-5 w-5 text-purple-600 dark:text-purple-400" />
          </div>
          <div>
            <p className="font-medium text-gray-900 dark:text-white">
              Meu Plano
            </p>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Ver detalhes do plano
            </p>
          </div>
          <ArrowRight className="h-5 w-5 text-gray-400 ml-auto" />
        </Link>
      </div>
    </div>
  );
}
