/**
 * Botão para pagamento online com Checkout Inline (Mercado Pago Bricks)
 */

import { useState } from 'react';
import { CreditCard, Copy, ExternalLink, QrCode, CheckCircle, Loader2, X, RefreshCw } from 'lucide-react';
import { useCriarCheckout, usePagamentosOnlinePorOS, usePublicKey, useAtualizarStatusPagamento, useGateways } from '../hooks/usePagamentoOnline';
import { StatusPagamentoOnline, getStatusColor, StatusPagamentoOnlineLabels } from '../types/pagamentoOnline';
import { showSuccess, showError } from '@/shared/utils/notifications';
import { formatCurrency } from '@/shared/utils/formatters';
import { CheckoutBrick } from './CheckoutBrick';

interface BotaoPagarOnlineProps {
  ordemServicoId: string;
  valorPendente: number;
  emailCliente?: string;
  nomeCliente?: string;
}

type ModalMode = 'link' | 'checkout' | null;

export function BotaoPagarOnline({
  ordemServicoId,
  valorPendente,
  emailCliente,
  nomeCliente,
}: BotaoPagarOnlineProps) {
  const [modalMode, setModalMode] = useState<ModalMode>(null);
  const [checkoutUrl, setCheckoutUrl] = useState<string | null>(null);
  const [codigoPix, setCodigoPix] = useState<string | null>(null);
  const [preferenceId, setPreferenceId] = useState<string | null>(null);

  const { data: gateways } = useGateways();
  const { data: publicKeyData } = usePublicKey();
  const { data: pagamentosOnline, refetch: refetchPagamentos } = usePagamentosOnlinePorOS(ordemServicoId);
  const criarCheckoutMutation = useCriarCheckout();
  const atualizarStatusMutation = useAtualizarStatusPagamento();

  const gatewayAtivo = gateways?.find((g) => g.ativo && g.statusValidacao === 'VALIDO');
  const ultimoPagamentoPendente = pagamentosOnline?.find(
    (p) => p.status === StatusPagamentoOnline.PENDENTE && !p.expirado
  );

  // Verificar se pode usar Checkout Bricks (tem public key configurada)
  const canUseBricks = !!publicKeyData?.publicKey;

  const handlePagarOnline = async () => {
    if (valorPendente <= 0) {
      showError('Não há valor pendente para pagamento');
      return;
    }

    try {
      const result = await criarCheckoutMutation.mutateAsync({
        ordemServicoId,
        valor: valorPendente,
        emailPagador: emailCliente,
        nomePagador: nomeCliente,
      });

      setCheckoutUrl(result.urlCheckout);
      setCodigoPix(result.codigoPix || null);
      setPreferenceId(result.preferenceId);

      // Se tem public key, abre checkout inline, senão mostra link
      if (canUseBricks) {
        setModalMode('checkout');
      } else {
        setModalMode('link');
        showSuccess('Link de pagamento gerado!');
      }

      refetchPagamentos();
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao gerar link de pagamento');
    }
  };

  const handleVerLinkExistente = () => {
    if (ultimoPagamentoPendente?.urlCheckout) {
      setCheckoutUrl(ultimoPagamentoPendente.urlCheckout);
      setCodigoPix(ultimoPagamentoPendente.codigoPix || null);
      setPreferenceId(ultimoPagamentoPendente.preferenceId || null);

      // Se tem public key e preference, abre checkout inline
      if (canUseBricks && ultimoPagamentoPendente.preferenceId) {
        setModalMode('checkout');
      } else {
        setModalMode('link');
      }
    }
  };

  const handleCopiarLink = () => {
    if (checkoutUrl) {
      navigator.clipboard.writeText(checkoutUrl);
      showSuccess('Link copiado!');
    }
  };

  const handleCopiarPix = () => {
    if (codigoPix) {
      navigator.clipboard.writeText(codigoPix);
      showSuccess('Código PIX copiado!');
    }
  };

  const handleAbrirCheckout = () => {
    if (checkoutUrl) {
      window.open(checkoutUrl, '_blank');
    }
  };

  const handleCheckoutSuccess = (_paymentId: string) => {
    refetchPagamentos();
    // Mantém modal aberto para mostrar sucesso
  };

  const handleCheckoutError = (error: string) => {
    console.error('Erro no checkout:', error);
  };

  const handleCloseModal = () => {
    setModalMode(null);
    refetchPagamentos();
  };

  const handleAtualizarStatus = async (pagamentoId: string) => {
    try {
      const result = await atualizarStatusMutation.mutateAsync(pagamentoId);
      if (result.status === StatusPagamentoOnline.APROVADO) {
        showSuccess('Pagamento confirmado!');
      } else if (result.status === StatusPagamentoOnline.REJEITADO) {
        showError('Pagamento foi rejeitado');
      } else {
        showSuccess('Status atualizado: ' + StatusPagamentoOnlineLabels[result.status]);
      }
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao atualizar status');
    }
  };

  // Se não tem gateway configurado, não mostra nada
  if (!gatewayAtivo) {
    return null;
  }

  return (
    <>
      <div className="space-y-3">
        {/* Botão principal */}
        <button
          onClick={ultimoPagamentoPendente ? handleVerLinkExistente : handlePagarOnline}
          disabled={criarCheckoutMutation.isPending || valorPendente <= 0}
          className="flex w-full items-center justify-center gap-2 rounded-lg bg-green-600 px-4 py-3 text-white hover:bg-green-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {criarCheckoutMutation.isPending ? (
            <Loader2 className="h-5 w-5 animate-spin" />
          ) : (
            <CreditCard className="h-5 w-5" />
          )}
          <span className="font-medium">
            {ultimoPagamentoPendente
              ? 'Continuar Pagamento'
              : `Pagar Online ${formatCurrency(valorPendente)}`}
          </span>
        </button>

        {/* Indicador de checkout inline */}
        {canUseBricks && (
          <p className="text-center text-xs text-gray-500 dark:text-gray-400">
            Pagamento seguro sem sair da plataforma
          </p>
        )}

        {/* Lista de pagamentos online recentes */}
        {pagamentosOnline && pagamentosOnline.length > 0 && (
          <div className="space-y-2">
            <p className="text-xs font-medium text-gray-500 dark:text-gray-400">
              Pagamentos Online Recentes:
            </p>
            {pagamentosOnline.slice(0, 3).map((po) => (
              <div
                key={po.id}
                className="flex items-center justify-between rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50 px-3 py-2"
              >
                <div className="flex items-center gap-2">
                  {po.status === StatusPagamentoOnline.APROVADO && (
                    <CheckCircle className="h-4 w-4 text-green-500" />
                  )}
                  <span className="text-sm font-medium text-gray-900 dark:text-white">
                    {formatCurrency(po.valor)}
                  </span>
                  <span className="text-xs text-gray-500 dark:text-gray-400">
                    {po.metodoPagamento || po.gatewayDescricao}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${getStatusColor(po.status)}`}
                  >
                    {StatusPagamentoOnlineLabels[po.status]}
                  </span>
                  {po.status === StatusPagamentoOnline.PENDENTE && (
                    <button
                      onClick={() => handleAtualizarStatus(po.id)}
                      disabled={atualizarStatusMutation.isPending}
                      className="rounded p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-600 dark:hover:bg-gray-600 dark:hover:text-gray-300 disabled:opacity-50"
                      title="Atualizar status do pagamento"
                    >
                      <RefreshCw
                        className={`h-3.5 w-3.5 ${atualizarStatusMutation.isPending ? 'animate-spin' : ''}`}
                      />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Modal de Checkout Inline (Bricks) */}
      {modalMode === 'checkout' && preferenceId && publicKeyData?.publicKey && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="relative w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-lg bg-white dark:bg-gray-800 shadow-xl">
            {/* Header */}
            <div className="sticky top-0 flex items-center justify-between border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4">
              <div className="flex items-center gap-2">
                <CreditCard className="h-5 w-5 text-green-600" />
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                  Pagamento Online
                </h3>
              </div>
              <button
                onClick={handleCloseModal}
                className="rounded-full p-1 hover:bg-gray-100 dark:hover:bg-gray-700"
              >
                <X className="h-5 w-5 text-gray-500" />
              </button>
            </div>

            {/* Checkout Brick */}
            <div className="p-4">
              <CheckoutBrick
                preferenceId={preferenceId}
                publicKey={publicKeyData.publicKey}
                valor={valorPendente}
                ordemServicoId={ordemServicoId}
                onSuccess={handleCheckoutSuccess}
                onError={handleCheckoutError}
                onClose={handleCloseModal}
              />
            </div>

            {/* Opção de usar link externo */}
            {checkoutUrl && (
              <div className="border-t border-gray-200 dark:border-gray-700 p-4">
                <button
                  onClick={() => setModalMode('link')}
                  className="text-sm text-blue-600 hover:text-blue-700 dark:text-blue-400"
                >
                  Prefiro usar o link externo
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Modal com link de pagamento (fallback) */}
      {modalMode === 'link' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-md rounded-lg bg-white dark:bg-gray-800 p-6 shadow-xl">
            <div className="text-center">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-green-100 dark:bg-green-900/30">
                <CreditCard className="h-6 w-6 text-green-600 dark:text-green-400" />
              </div>
              <h3 className="mt-4 text-lg font-semibold text-gray-900 dark:text-white">
                Link de Pagamento
              </h3>
              <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
                Envie o link para o cliente ou use o QR Code PIX
              </p>
            </div>

            <div className="mt-6 space-y-4">
              {/* Link do Checkout */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Link de Pagamento
                </label>
                <div className="mt-1 flex rounded-lg border border-gray-300 dark:border-gray-600">
                  <input
                    type="text"
                    value={checkoutUrl || ''}
                    readOnly
                    className="flex-1 rounded-l-lg border-0 bg-gray-50 dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white focus:outline-none"
                  />
                  <button
                    onClick={handleCopiarLink}
                    className="rounded-r-lg border-l border-gray-300 dark:border-gray-600 bg-gray-100 dark:bg-gray-600 px-3 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-500"
                  >
                    <Copy className="h-4 w-4" />
                  </button>
                </div>
              </div>

              {/* Código PIX */}
              {codigoPix && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    <QrCode className="mr-1 inline h-4 w-4" />
                    Código PIX (Copia e Cola)
                  </label>
                  <div className="mt-1 flex rounded-lg border border-gray-300 dark:border-gray-600">
                    <input
                      type="text"
                      value={codigoPix}
                      readOnly
                      className="flex-1 rounded-l-lg border-0 bg-gray-50 dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white focus:outline-none"
                    />
                    <button
                      onClick={handleCopiarPix}
                      className="rounded-r-lg border-l border-gray-300 dark:border-gray-600 bg-gray-100 dark:bg-gray-600 px-3 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-500"
                    >
                      <Copy className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              )}

              {/* Valor */}
              <div className="rounded-lg bg-gray-100 dark:bg-gray-700 p-4 text-center">
                <p className="text-sm text-gray-600 dark:text-gray-400">Valor a pagar</p>
                <p className="text-2xl font-bold text-gray-900 dark:text-white">
                  {formatCurrency(valorPendente)}
                </p>
              </div>
            </div>

            <div className="mt-6 flex gap-3">
              <button
                onClick={handleCloseModal}
                className="flex-1 rounded-lg border border-gray-300 dark:border-gray-600 px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
              >
                Fechar
              </button>
              <button
                onClick={handleAbrirCheckout}
                className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
              >
                <ExternalLink className="h-4 w-4" />
                Abrir Checkout
              </button>
            </div>

            {/* Botão para voltar ao checkout inline */}
            {canUseBricks && preferenceId && (
              <div className="mt-4 text-center">
                <button
                  onClick={() => setModalMode('checkout')}
                  className="text-sm text-blue-600 hover:text-blue-700 dark:text-blue-400"
                >
                  Pagar aqui mesmo (sem sair)
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}
