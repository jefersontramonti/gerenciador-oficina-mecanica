/**
 * Página de detalhes de uma fatura com opção de pagamento
 */

import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import {
  ArrowLeft,
  Download,
  CreditCard,
  QrCode,
  Copy,
  CheckCircle,
  AlertCircle,
  Clock,
  ExternalLink,
  Loader2,
} from 'lucide-react';
import {
  useFaturaDetalhe,
  useIniciarPagamento,
  useDownloadFaturaPdf,
} from '../hooks/useMinhaContaFaturas';
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

const formatDateTime = (dateString?: string): string => {
  if (!dateString) return '-';
  try {
    return format(new Date(dateString), "dd/MM/yyyy 'às' HH:mm", { locale: ptBR });
  } catch {
    return '-';
  }
};

/**
 * Converte Base64 puro para data URI de imagem PNG
 * O Mercado Pago retorna o QR Code como Base64 puro, sem o prefixo
 */
const getBase64ImageSrc = (base64: string | undefined): string => {
  if (!base64) return '';
  if (base64.startsWith('data:image')) return base64;
  return `data:image/png;base64,${base64}`;
};

export function FaturaDetalhePage() {
  const { id } = useParams<{ id: string }>();
  const [showPixModal, setShowPixModal] = useState(false);
  const [pixCopied, setPixCopied] = useState(false);

  const { data: fatura, isLoading, error } = useFaturaDetalhe(id);
  const iniciarPagamentoMutation = useIniciarPagamento();
  const downloadPdfMutation = useDownloadFaturaPdf();

  const handlePagar = async (metodo: 'PIX' | 'CHECKOUT') => {
    if (!id) return;

    try {
      const resultado = await iniciarPagamentoMutation.mutateAsync({
        faturaId: id,
        metodoPagamento: metodo,
      });

      if (resultado.status === 'CREATED') {
        if (metodo === 'PIX' && resultado.pixQrCode) {
          setShowPixModal(true);
        } else if (resultado.initPoint) {
          // Redireciona para checkout do Mercado Pago
          window.open(resultado.initPoint, '_blank');
        } else {
          // Pagamento criado mas sem URL - provavelmente modo sandbox
          alert('Pagamento registrado. O link de pagamento será disponibilizado em breve.');
        }
      } else {
        // Status ERROR - mostrar mensagem amigável
        alert(resultado.message || 'Não foi possível iniciar o pagamento. Tente novamente.');
      }
    } catch (err: any) {
      alert(err.response?.data?.message || err.message || 'Erro ao iniciar pagamento');
    }
  };

  const handleDownloadPdf = () => {
    if (id) {
      downloadPdfMutation.mutate(id);
    }
  };

  const handleCopyPix = (text: string) => {
    navigator.clipboard.writeText(text);
    setPixCopied(true);
    setTimeout(() => setPixCopied(false), 2000);
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[50vh]">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 dark:border-blue-400 border-t-transparent"></div>
      </div>
    );
  }

  if (error || !fatura) {
    return (
      <div className="p-4 sm:p-6">
        <div className="rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400">
          <p className="font-semibold">Erro ao carregar fatura</p>
          <p className="mt-1 text-sm">{error?.message || 'Fatura não encontrada.'}</p>
          <Link
            to="/minha-conta/faturas"
            className="mt-3 inline-flex items-center gap-2 text-sm text-red-300 hover:text-red-200"
          >
            <ArrowLeft className="h-4 w-4" />
            Voltar para faturas
          </Link>
        </div>
      </div>
    );
  }

  const status = fatura.status as StatusFatura;

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link
            to="/minha-conta/faturas"
            className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-600 dark:text-gray-400"
          >
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
              Fatura {fatura.numero}
            </h1>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              Referência: {fatura.mesReferenciaFormatado}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={handleDownloadPdf}
            disabled={downloadPdfMutation.isPending}
            className="flex items-center gap-2 px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
          >
            <Download className="h-4 w-4" />
            <span className="hidden sm:inline">Download PDF</span>
          </button>
        </div>
      </div>

      {/* Status e Alerta */}
      {fatura.vencida && fatura.pagavel && (
        <div className="rounded-lg border border-red-300 dark:border-red-800 bg-red-50 dark:bg-red-900/30 p-4">
          <div className="flex items-start gap-3">
            <AlertCircle className="h-5 w-5 text-red-600 dark:text-red-400 shrink-0 mt-0.5" />
            <div>
              <h3 className="font-semibold text-red-800 dark:text-red-300">
                Fatura vencida
              </h3>
              <p className="mt-1 text-sm text-red-700 dark:text-red-400">
                Esta fatura está vencida há{' '}
                {Math.abs(fatura.diasAteVencimento || 0)} dia(s). Regularize o
                pagamento para evitar a suspensão dos serviços.
              </p>
            </div>
          </div>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Dados da Fatura */}
        <div className="lg:col-span-2 space-y-6">
          {/* Informações */}
          <div className="rounded-lg bg-white dark:bg-gray-800 shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
            <div className="px-4 sm:px-6 py-4 border-b border-gray-200 dark:border-gray-700">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                Informações da Fatura
              </h2>
            </div>
            <div className="p-4 sm:p-6">
              <dl className="grid gap-4 sm:grid-cols-2">
                <div>
                  <dt className="text-sm text-gray-500 dark:text-gray-400">Número</dt>
                  <dd className="text-sm font-medium text-gray-900 dark:text-white">
                    {fatura.numero}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-gray-500 dark:text-gray-400">Status</dt>
                  <dd>
                    <span
                      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${StatusFaturaCores[status].bg} ${StatusFaturaCores[status].text}`}
                    >
                      {StatusFaturaLabels[status]}
                    </span>
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-gray-500 dark:text-gray-400">
                    Mês de Referência
                  </dt>
                  <dd className="text-sm font-medium text-gray-900 dark:text-white">
                    {fatura.mesReferenciaFormatado}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-gray-500 dark:text-gray-400">Plano</dt>
                  <dd className="text-sm font-medium text-gray-900 dark:text-white">
                    {fatura.planoCodigo}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-gray-500 dark:text-gray-400">
                    Data de Emissão
                  </dt>
                  <dd className="text-sm font-medium text-gray-900 dark:text-white">
                    {formatDate(fatura.dataEmissao)}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-gray-500 dark:text-gray-400">
                    Data de Vencimento
                  </dt>
                  <dd className="text-sm font-medium text-gray-900 dark:text-white">
                    {formatDate(fatura.dataVencimento)}
                  </dd>
                </div>
                {fatura.dataPagamento && (
                  <div>
                    <dt className="text-sm text-gray-500 dark:text-gray-400">
                      Data de Pagamento
                    </dt>
                    <dd className="text-sm font-medium text-green-600 dark:text-green-400">
                      {formatDateTime(fatura.dataPagamento)}
                    </dd>
                  </div>
                )}
                {fatura.metodoPagamento && (
                  <div>
                    <dt className="text-sm text-gray-500 dark:text-gray-400">
                      Método de Pagamento
                    </dt>
                    <dd className="text-sm font-medium text-gray-900 dark:text-white">
                      {fatura.metodoPagamento}
                    </dd>
                  </div>
                )}
              </dl>
            </div>
          </div>

          {/* Itens */}
          <div className="rounded-lg bg-white dark:bg-gray-800 shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
            <div className="px-4 sm:px-6 py-4 border-b border-gray-200 dark:border-gray-700">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                Itens
              </h2>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="px-4 sm:px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Descrição
                    </th>
                    <th className="px-4 sm:px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Qtd
                    </th>
                    <th className="px-4 sm:px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Valor Unit.
                    </th>
                    <th className="px-4 sm:px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                      Total
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                  {fatura.itens.map((item) => (
                    <tr key={item.id}>
                      <td className="px-4 sm:px-6 py-4 text-sm text-gray-900 dark:text-white">
                        {item.descricao}
                      </td>
                      <td className="px-4 sm:px-6 py-4 text-sm text-right text-gray-600 dark:text-gray-400">
                        {item.quantidade}
                      </td>
                      <td className="px-4 sm:px-6 py-4 text-sm text-right text-gray-600 dark:text-gray-400">
                        {formatCurrency(item.valorUnitario)}
                      </td>
                      <td className="px-4 sm:px-6 py-4 text-sm text-right font-medium text-gray-900 dark:text-white">
                        {formatCurrency(item.valorTotal)}
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <td
                      colSpan={3}
                      className="px-4 sm:px-6 py-3 text-sm font-medium text-right text-gray-700 dark:text-gray-300"
                    >
                      Subtotal
                    </td>
                    <td className="px-4 sm:px-6 py-3 text-sm font-medium text-right text-gray-900 dark:text-white">
                      {formatCurrency(fatura.valorTotal)}
                    </td>
                  </tr>
                  {fatura.desconto != null && fatura.desconto > 0 && (
                    <tr>
                      <td
                        colSpan={3}
                        className="px-4 sm:px-6 py-3 text-sm font-medium text-right text-green-700 dark:text-green-400"
                      >
                        Desconto
                      </td>
                      <td className="px-4 sm:px-6 py-3 text-sm font-medium text-right text-green-700 dark:text-green-400">
                        - {formatCurrency(fatura.desconto)}
                      </td>
                    </tr>
                  )}
                  <tr className="bg-gray-100 dark:bg-gray-600">
                    <td
                      colSpan={3}
                      className="px-4 sm:px-6 py-4 text-base font-bold text-right text-gray-900 dark:text-white"
                    >
                      Total a Pagar
                    </td>
                    <td className="px-4 sm:px-6 py-4 text-base font-bold text-right text-gray-900 dark:text-white">
                      {formatCurrency(fatura.valorFinal)}
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>

          {/* Observações */}
          {fatura.observacao && (
            <div className="rounded-lg bg-white dark:bg-gray-800 shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
              <div className="px-4 sm:px-6 py-4 border-b border-gray-200 dark:border-gray-700">
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                  Observações
                </h2>
              </div>
              <div className="p-4 sm:p-6">
                <p className="text-sm text-gray-600 dark:text-gray-400 whitespace-pre-wrap">
                  {fatura.observacao}
                </p>
              </div>
            </div>
          )}
        </div>

        {/* Sidebar - Pagamento */}
        <div className="space-y-6">
          {/* Card de Pagamento */}
          <div className="rounded-lg bg-white dark:bg-gray-800 shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
            <div className="px-4 py-4 border-b border-gray-200 dark:border-gray-700">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                Pagamento
              </h2>
            </div>
            <div className="p-4 space-y-4">
              <div className="text-center py-4">
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  Valor total
                </p>
                <p className="text-3xl font-bold text-gray-900 dark:text-white">
                  {formatCurrency(fatura.valorFinal)}
                </p>
                {fatura.pagavel && (
                  <p
                    className={`mt-2 text-sm font-medium ${
                      fatura.vencida
                        ? 'text-red-600 dark:text-red-400'
                        : fatura.diasAteVencimento !== null &&
                          fatura.diasAteVencimento <= 3
                        ? 'text-yellow-600 dark:text-yellow-400'
                        : 'text-gray-600 dark:text-gray-400'
                    }`}
                  >
                    Vencimento: {formatDate(fatura.dataVencimento)}
                  </p>
                )}
              </div>

              {fatura.pagavel ? (
                <div className="space-y-3">
                  {/* PIX */}
                  <button
                    onClick={() => handlePagar('PIX')}
                    disabled={iniciarPagamentoMutation.isPending}
                    className="w-full flex items-center justify-center gap-2 px-4 py-3 rounded-lg bg-green-600 dark:bg-green-700 text-white hover:bg-green-700 dark:hover:bg-green-600 disabled:opacity-50 font-medium"
                  >
                    {iniciarPagamentoMutation.isPending ? (
                      <Loader2 className="h-5 w-5 animate-spin" />
                    ) : (
                      <QrCode className="h-5 w-5" />
                    )}
                    Pagar com PIX
                  </button>

                  {/* Cartão/Boleto - Checkout Mercado Pago */}
                  <button
                    onClick={() => handlePagar('CHECKOUT')}
                    disabled={iniciarPagamentoMutation.isPending}
                    className="w-full flex items-center justify-center gap-2 px-4 py-3 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 font-medium"
                  >
                    {iniciarPagamentoMutation.isPending ? (
                      <Loader2 className="h-5 w-5 animate-spin" />
                    ) : (
                      <CreditCard className="h-5 w-5" />
                    )}
                    Cartão ou Boleto
                  </button>

                  <p className="text-xs text-center text-gray-500 dark:text-gray-400">
                    Pagamento processado pelo Mercado Pago
                  </p>
                </div>
              ) : status === StatusFatura.PAGO ? (
                <div className="text-center py-4">
                  <CheckCircle className="h-12 w-12 text-green-500 mx-auto mb-2" />
                  <p className="text-sm font-medium text-green-600 dark:text-green-400">
                    Fatura paga
                  </p>
                  {fatura.dataPagamento && (
                    <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      em {formatDateTime(fatura.dataPagamento)}
                    </p>
                  )}
                </div>
              ) : (
                <div className="text-center py-4">
                  <Clock className="h-12 w-12 text-gray-400 mx-auto mb-2" />
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    Esta fatura não pode ser paga no momento.
                  </p>
                </div>
              )}

              {/* Link existente */}
              {fatura.linkPagamento && fatura.pagavel && (
                <div className="pt-3 border-t border-gray-200 dark:border-gray-700">
                  <a
                    href={fatura.linkPagamento}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center justify-center gap-2 text-sm text-blue-600 dark:text-blue-400 hover:underline"
                  >
                    <ExternalLink className="h-4 w-4" />
                    Abrir link de pagamento
                  </a>
                </div>
              )}
            </div>
          </div>

        </div>
      </div>

      {/* Modal PIX */}
      {showPixModal && iniciarPagamentoMutation.data && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-gray-800 rounded-lg p-6 max-w-md w-full shadow-xl">
            <div className="text-center">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                Pagamento via PIX
              </h3>

              {iniciarPagamentoMutation.data.pixQrCode ? (
                <>
                  <div className="bg-white p-4 rounded-lg inline-block mb-4">
                    <img
                      src={getBase64ImageSrc(iniciarPagamentoMutation.data.pixQrCode)}
                      alt="QR Code PIX"
                      className="w-48 h-48"
                    />
                  </div>

                  <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                    Escaneie o QR Code acima com seu app de banco
                  </p>

                  {iniciarPagamentoMutation.data.pixQrCodeText && (
                    <div className="mb-4">
                      <p className="text-xs text-gray-500 dark:text-gray-400 mb-2">
                        Ou copie o código PIX:
                      </p>
                      <div className="flex items-center gap-2">
                        <input
                          type="text"
                          readOnly
                          value={iniciarPagamentoMutation.data.pixQrCodeText}
                          className="flex-1 px-3 py-2 text-xs bg-gray-100 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-900 dark:text-white"
                        />
                        <button
                          onClick={() =>
                            handleCopyPix(
                              iniciarPagamentoMutation.data!.pixQrCodeText!
                            )
                          }
                          className="flex items-center gap-1 px-3 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 text-sm"
                        >
                          {pixCopied ? (
                            <CheckCircle className="h-4 w-4" />
                          ) : (
                            <Copy className="h-4 w-4" />
                          )}
                        </button>
                      </div>
                      {pixCopied && (
                        <p className="text-xs text-green-600 dark:text-green-400 mt-1">
                          Código copiado!
                        </p>
                      )}
                    </div>
                  )}

                  {iniciarPagamentoMutation.data.pixExpirationDate && (
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      Válido até:{' '}
                      {formatDateTime(
                        iniciarPagamentoMutation.data.pixExpirationDate
                      )}
                    </p>
                  )}
                </>
              ) : (
                <p className="text-gray-500 dark:text-gray-400">
                  Não foi possível gerar o QR Code. Tente novamente.
                </p>
              )}

              <button
                onClick={() => setShowPixModal(false)}
                className="mt-6 w-full px-4 py-2 bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-gray-200 rounded-lg hover:bg-gray-300 dark:hover:bg-gray-600"
              >
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
