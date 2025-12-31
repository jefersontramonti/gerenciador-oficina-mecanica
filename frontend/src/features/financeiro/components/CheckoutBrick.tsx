/**
 * Componente de Checkout Inline usando Mercado Pago Bricks
 * Permite pagamento sem redirecionamento externo
 * Suporta tema claro/escuro da aplicação
 */

import { useEffect, useState, useCallback, useMemo, useRef } from 'react';
import { initMercadoPago, Payment } from '@mercadopago/sdk-react';
import { useQueryClient } from '@tanstack/react-query';
import { Loader2, AlertCircle, CheckCircle, XCircle, Copy, QrCode } from 'lucide-react';
import { showSuccess, showError } from '@/shared/utils/notifications';
import { formatCurrency } from '@/shared/utils/formatters';
import { api } from '@/shared/services/api';
import { useTheme } from '@/shared/contexts/ThemeContext';
import { StatusPagamentoOnline } from '../types/pagamentoOnline';

interface CheckoutBrickProps {
  preferenceId: string;
  publicKey: string;
  valor: number;
  ordemServicoId: string;
  onSuccess: (paymentId: string) => void;
  onError: (error: string) => void;
  onClose: () => void;
}

type PaymentStatus = 'idle' | 'loading' | 'ready' | 'processing' | 'success' | 'success_pix' | 'success_pix_approved' | 'success_boleto' | 'error';

export function CheckoutBrick({
  preferenceId,
  publicKey,
  valor,
  ordemServicoId,
  onSuccess,
  onError,
  onClose,
}: CheckoutBrickProps) {
  const [status, setStatus] = useState<PaymentStatus>('loading');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [sdkInitialized, setSdkInitialized] = useState(false);
  const { theme } = useTheme();
  const queryClient = useQueryClient();

  // Estado para dados do PIX
  const [pixData, setPixData] = useState<{
    qrCodeBase64: string | null;
    qrCode: string | null;
  }>({ qrCodeBase64: null, qrCode: null });

  // Estado para dados do Boleto
  const [boletoData, setBoletoData] = useState<{
    barcodeContent: string | null;
    externalResourceUrl: string | null;
  }>({ barcodeContent: null, externalResourceUrl: null });

  // Estado para ID do pagamento online (para polling)
  const [pagamentoOnlineId, setPagamentoOnlineId] = useState<string | null>(null);
  const pollingIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const autoCloseTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Inicializar SDK do Mercado Pago
  useEffect(() => {
    if (!publicKey || sdkInitialized) return;

    try {
      initMercadoPago(publicKey, {
        locale: 'pt-BR',
      });
      setSdkInitialized(true);
      setStatus('ready');
    } catch (err) {
      console.error('Erro ao inicializar Mercado Pago SDK:', err);
      setStatus('error');
      setErrorMessage('Erro ao inicializar o sistema de pagamento');
    }
  }, [publicKey, sdkInitialized]);

  // Polling para verificar status do PIX
  useEffect(() => {
    if (status !== 'success_pix' || !pagamentoOnlineId) return;

    const checkPaymentStatus = async () => {
      try {
        const response = await api.post(`/pagamentos-online/${pagamentoOnlineId}/atualizar-status`);
        const pagamento = response.data;

        if (pagamento.status === StatusPagamentoOnline.APROVADO) {
          // Limpar polling
          if (pollingIntervalRef.current) {
            clearInterval(pollingIntervalRef.current);
            pollingIntervalRef.current = null;
          }

          // Invalidar cache dos pagamentos para atualizar a lista
          queryClient.invalidateQueries({ queryKey: ['pagamentos'] });
          queryClient.invalidateQueries({ queryKey: ['resumo-financeiro'] });
          queryClient.invalidateQueries({ queryKey: ['pagamentos-online'] });

          // Mostrar mensagem de aprovado
          setStatus('success_pix_approved');
          showSuccess('Pagamento PIX confirmado!');

          // Auto-fechar após 3 segundos
          autoCloseTimeoutRef.current = setTimeout(() => {
            onClose();
          }, 3000);
        }
      } catch (error) {
        console.error('Erro ao verificar status:', error);
      }
    };

    // Verificar imediatamente e depois a cada 5 segundos
    checkPaymentStatus();
    pollingIntervalRef.current = setInterval(checkPaymentStatus, 5000);

    // Cleanup
    return () => {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
      }
      if (autoCloseTimeoutRef.current) {
        clearTimeout(autoCloseTimeoutRef.current);
      }
    };
  }, [status, pagamentoOnlineId, onClose, queryClient]);

  // Callback quando o pagamento é submetido
  // O Payment Brick retorna os dados do formulário que precisam ser enviados ao backend
  // para criar o pagamento via API do Mercado Pago
  const handleOnSubmit = useCallback(
    async (formData: any) => {
      setStatus('processing');

      console.log('Payment Brick formData:', formData);

      try {
        // Identificar o método de pagamento selecionado
        const selectedMethod = formData?.selectedPaymentMethod || formData?.paymentType;
        const isPix = selectedMethod === 'bank_transfer' || selectedMethod === 'pix';
        const isBoleto = selectedMethod === 'ticket';

        console.log('Método selecionado:', selectedMethod, 'isPix:', isPix, 'isBoleto:', isBoleto);

        // SEMPRE enviar para o backend processar e criar o pagamento
        const response = await api.post('/pagamentos-online/processar-brick', {
          formData,
          ordemServicoId,
          preferenceId,
        });

        console.log('Resposta do backend:', response.data);

        const {
          status: backendStatus,
          paymentId: backendPaymentId,
          pagamentoOnlineId: backendPagamentoOnlineId,
          statusDetail: backendStatusDetail,
          payment_method_id: backendPaymentMethod,
          point_of_interaction: backendPointOfInteraction,
          transaction_details: backendTransactionDetails,
          error: backendError,
        } = response.data;

        // Verificar se houve erro
        if (backendError || backendStatus === 'error') {
          setStatus('error');
          setErrorMessage(backendError || 'Erro ao processar pagamento');
          onError(backendError || 'Erro ao processar pagamento');
          return;
        }

        // Determinar método de pagamento final
        const finalIsPix = backendPaymentMethod === 'pix' || isPix;
        const finalIsBoleto = backendPaymentMethod === 'bolbradesco' || backendPaymentMethod === 'ticket' || isBoleto;

        if (backendStatus === 'approved') {
          setStatus('success');
          showSuccess('Pagamento aprovado!');
          onSuccess(backendPaymentId?.toString() || '');
        } else if (backendStatus === 'pending' || backendStatus === 'in_process') {
          if (finalIsPix) {
            // Extrair dados do PIX da resposta do backend
            const backendTransactionData = backendPointOfInteraction?.transaction_data;
            const backendQrCodeBase64 = backendTransactionData?.qr_code_base64 || null;
            const backendQrCode = backendTransactionData?.qr_code || null;

            console.log('PIX Data do Backend:', {
              hasQrCodeBase64: !!backendQrCodeBase64,
              hasQrCode: !!backendQrCode,
              transactionData: backendTransactionData
            });

            setPixData({ qrCodeBase64: backendQrCodeBase64, qrCode: backendQrCode });
            setPagamentoOnlineId(backendPagamentoOnlineId || null);
            setStatus('success_pix');
            showSuccess('PIX gerado! Escaneie o QR Code ou copie o código para pagar.');
            onSuccess(backendPaymentId?.toString() || '');
          } else if (finalIsBoleto) {
            const backendBarcodeContent = backendPointOfInteraction?.transaction_data?.barcode?.content || null;
            const backendExternalResourceUrl = backendTransactionDetails?.external_resource_url || null;

            setBoletoData({ barcodeContent: backendBarcodeContent, externalResourceUrl: backendExternalResourceUrl });
            setStatus('success_boleto');
            showSuccess('Boleto gerado! Pague até a data de vencimento.');
            onSuccess(backendPaymentId?.toString() || '');
          } else {
            setStatus('success');
            showSuccess('Pagamento em processamento. Você será notificado quando for aprovado.');
            onSuccess(backendPaymentId?.toString() || '');
          }
        } else if (backendStatus === 'rejected') {
          setStatus('error');
          const rejectReason = getRejectReason(backendStatusDetail || '');
          setErrorMessage(rejectReason);
          onError(rejectReason);
        } else {
          // Status desconhecido - pode ser que o pagamento não foi criado
          setStatus('error');
          setErrorMessage('Não foi possível processar o pagamento. Verifique os dados e tente novamente.');
          onError('Status desconhecido: ' + backendStatus);
        }
      } catch (err: any) {
        console.error('Erro ao processar pagamento:', err);
        setStatus('error');
        const message = err.response?.data?.message || err.response?.data?.error || 'Erro ao processar pagamento';
        setErrorMessage(message);
        onError(message);
      }
    },
    [ordemServicoId, preferenceId, onSuccess, onError]
  );

  // Traduz o motivo de rejeição para português
  const getRejectReason = (statusDetail: string): string => {
    const reasons: Record<string, string> = {
      'cc_rejected_bad_filled_card_number': 'Número do cartão incorreto',
      'cc_rejected_bad_filled_date': 'Data de validade incorreta',
      'cc_rejected_bad_filled_other': 'Dados do cartão incorretos',
      'cc_rejected_bad_filled_security_code': 'Código de segurança incorreto',
      'cc_rejected_blacklist': 'Cartão não permitido',
      'cc_rejected_call_for_authorize': 'Autorização necessária. Ligue para o banco.',
      'cc_rejected_card_disabled': 'Cartão desabilitado. Ligue para o banco.',
      'cc_rejected_card_error': 'Erro no cartão. Tente outro.',
      'cc_rejected_duplicated_payment': 'Pagamento duplicado',
      'cc_rejected_high_risk': 'Pagamento recusado por segurança',
      'cc_rejected_insufficient_amount': 'Saldo insuficiente',
      'cc_rejected_invalid_installments': 'Parcelas inválidas',
      'cc_rejected_max_attempts': 'Muitas tentativas. Tente mais tarde.',
      'cc_rejected_other_reason': 'Pagamento recusado. Tente outro cartão.',
      'pending_contingency': 'Processando pagamento...',
      'pending_review_manual': 'Pagamento em análise',
    };
    return reasons[statusDetail] || 'Pagamento não aprovado. Tente novamente.';
  };

  // Callback para erros do Brick
  const handleOnError = useCallback(
    (error: any) => {
      console.error('Erro no Payment Brick:', error);
      setStatus('error');
      setErrorMessage('Erro no formulário de pagamento');
      showError('Erro no formulário de pagamento');
    },
    []
  );

  // Callback quando o Brick está pronto
  const handleOnReady = useCallback(() => {
    setStatus('ready');
  }, []);

  // Customização do Brick baseada no tema
  // Documentação: https://www.mercadopago.com.br/developers/pt/docs/checkout-bricks/payment-brick/visual-customizations/change-appearance
  const customization = useMemo(() => {
    const isDark = theme === 'dark';
    const themeValue = isDark ? 'dark' : 'default';

    return {
      paymentMethods: {
        creditCard: 'all' as const,
        debitCard: 'all' as const,
        ticket: 'all' as const, // Boleto
        bankTransfer: 'all' as const, // PIX
        mercadoPago: 'all' as const, // Saldo Mercado Pago
      },
      visual: {
        style: {
          // Usa o tema 'dark' nativo do Mercado Pago para tema escuro
          theme: themeValue as 'dark' | 'default',
          // Apenas propriedades válidas do SDK
          customVariables: {
            // Cores principais
            formBackgroundColor: isDark ? '#1f2937' : '#ffffff',
            baseColor: isDark ? '#3b82f6' : '#2563eb',
            // Cores de texto
            textPrimaryColor: isDark ? '#f9fafb' : '#111827',
            textSecondaryColor: isDark ? '#9ca3af' : '#6b7280',
            // Cores de feedback
            errorColor: isDark ? '#ef4444' : '#dc2626',
            successColor: isDark ? '#22c55e' : '#16a34a',
            // Border radius
            borderRadiusSmall: '4px',
            borderRadiusMedium: '8px',
            borderRadiusLarge: '12px',
          },
        },
        hideFormTitle: false,
        hidePaymentButton: false,
      },
    };
  }, [theme]);

  // Inicialização para Payment Brick
  const initialization = useMemo(
    () => ({
      amount: valor,
      preferenceId: preferenceId,
    }),
    [valor, preferenceId]
  );

  if (status === 'loading') {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <Loader2 className="h-12 w-12 animate-spin text-blue-500" />
        <p className="mt-4 text-gray-600 dark:text-gray-400">Carregando formulário de pagamento...</p>
      </div>
    );
  }

  if (status === 'error' && !sdkInitialized) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <AlertCircle className="h-12 w-12 text-red-500" />
        <p className="mt-4 text-red-600 dark:text-red-400">{errorMessage || 'Erro ao carregar'}</p>
        <button
          onClick={onClose}
          className="mt-4 rounded-lg bg-gray-600 px-4 py-2 text-white hover:bg-gray-700"
        >
          Fechar
        </button>
      </div>
    );
  }

  if (status === 'success') {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <CheckCircle className="h-16 w-16 text-green-500" />
        <h3 className="mt-4 text-xl font-semibold text-gray-900 dark:text-white">
          Pagamento Aprovado!
        </h3>
        <p className="mt-2 text-gray-600 dark:text-gray-400">
          Seu pagamento de {formatCurrency(valor)} foi processado com sucesso.
        </p>
        <button
          onClick={onClose}
          className="mt-6 rounded-lg bg-green-600 px-6 py-2 text-white hover:bg-green-700"
        >
          Concluir
        </button>
      </div>
    );
  }

  if (status === 'success_pix') {
    const handleCopyPixCode = () => {
      if (pixData.qrCode) {
        navigator.clipboard.writeText(pixData.qrCode);
        showSuccess('Código PIX copiado!');
      }
    };

    return (
      <div className="flex flex-col items-center justify-center py-6">
        <div className="rounded-full bg-green-100 dark:bg-green-900/30 p-3">
          <CheckCircle className="h-10 w-10 text-green-500" />
        </div>
        <h3 className="mt-3 text-lg font-semibold text-gray-900 dark:text-white">
          PIX Gerado com Sucesso!
        </h3>

        {/* QR Code Image */}
        {pixData.qrCodeBase64 && (
          <div className="mt-4 rounded-lg bg-white p-4 shadow-md">
            <img
              src={`data:image/png;base64,${pixData.qrCodeBase64}`}
              alt="QR Code PIX"
              className="h-48 w-48"
            />
          </div>
        )}

        {/* Se não tem QR Code mas tem o código */}
        {!pixData.qrCodeBase64 && pixData.qrCode && (
          <div className="mt-4 flex items-center justify-center rounded-lg bg-gray-100 dark:bg-gray-700 p-6">
            <QrCode className="h-16 w-16 text-gray-400" />
          </div>
        )}

        {/* Valor */}
        <div className="mt-4 rounded-lg bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 px-6 py-3 text-center">
          <p className="text-lg font-bold text-green-800 dark:text-green-300">
            {formatCurrency(valor)}
          </p>
        </div>

        {/* Código PIX Copia e Cola */}
        {pixData.qrCode && (
          <div className="mt-4 w-full">
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              <QrCode className="mr-1 inline h-4 w-4" />
              Código PIX (Copia e Cola)
            </label>
            <div className="flex rounded-lg border border-gray-300 dark:border-gray-600 overflow-hidden">
              <input
                type="text"
                value={pixData.qrCode}
                readOnly
                className="flex-1 border-0 bg-gray-50 dark:bg-gray-700 px-3 py-2 text-xs text-gray-900 dark:text-white focus:outline-none"
              />
              <button
                onClick={handleCopyPixCode}
                className="flex items-center gap-1 border-l border-gray-300 dark:border-gray-600 bg-gray-100 dark:bg-gray-600 px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-500"
              >
                <Copy className="h-4 w-4" />
                <span className="text-sm">Copiar</span>
              </button>
            </div>
          </div>
        )}

        {/* Aviso */}
        <div className="mt-4 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 p-3 text-center">
          <p className="text-xs text-amber-700 dark:text-amber-400">
            O pagamento será confirmado automaticamente após o PIX ser realizado.
          </p>
        </div>

        <button
          onClick={onClose}
          className="mt-4 rounded-lg bg-green-600 px-6 py-2 text-white hover:bg-green-700"
        >
          Fechar
        </button>
      </div>
    );
  }

  if (status === 'success_pix_approved') {
    return (
      <div className="flex flex-col items-center justify-center py-8">
        <div className="rounded-full bg-green-100 dark:bg-green-900/30 p-4">
          <CheckCircle className="h-16 w-16 text-green-500" />
        </div>
        <h3 className="mt-4 text-xl font-bold text-gray-900 dark:text-white">
          Pagamento Aprovado!
        </h3>
        <p className="mt-2 text-gray-600 dark:text-gray-400 text-center">
          Seu pagamento PIX de {formatCurrency(valor)} foi confirmado com sucesso.
        </p>

        <div className="mt-6 rounded-lg bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 px-8 py-4 text-center">
          <p className="text-sm text-green-700 dark:text-green-400">
            Esta janela será fechada automaticamente...
          </p>
        </div>

        <button
          onClick={onClose}
          className="mt-4 rounded-lg bg-green-600 px-8 py-2 text-white hover:bg-green-700"
        >
          Fechar Agora
        </button>
      </div>
    );
  }

  if (status === 'success_boleto') {
    const handleCopyBarcode = () => {
      if (boletoData.barcodeContent) {
        navigator.clipboard.writeText(boletoData.barcodeContent);
        showSuccess('Código de barras copiado!');
      }
    };

    const handleOpenBoleto = () => {
      if (boletoData.externalResourceUrl) {
        window.open(boletoData.externalResourceUrl, '_blank');
      }
    };

    return (
      <div className="flex flex-col items-center justify-center py-6">
        <div className="rounded-full bg-blue-100 dark:bg-blue-900/30 p-3">
          <CheckCircle className="h-10 w-10 text-blue-500" />
        </div>
        <h3 className="mt-3 text-lg font-semibold text-gray-900 dark:text-white">
          Boleto Gerado com Sucesso!
        </h3>

        {/* Valor */}
        <div className="mt-4 rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 px-6 py-3 text-center">
          <p className="text-lg font-bold text-blue-800 dark:text-blue-300">
            {formatCurrency(valor)}
          </p>
        </div>

        {/* Código de Barras */}
        {boletoData.barcodeContent && (
          <div className="mt-4 w-full">
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Código de Barras
            </label>
            <div className="flex rounded-lg border border-gray-300 dark:border-gray-600 overflow-hidden">
              <input
                type="text"
                value={boletoData.barcodeContent}
                readOnly
                className="flex-1 border-0 bg-gray-50 dark:bg-gray-700 px-3 py-2 text-xs text-gray-900 dark:text-white focus:outline-none font-mono"
              />
              <button
                onClick={handleCopyBarcode}
                className="flex items-center gap-1 border-l border-gray-300 dark:border-gray-600 bg-gray-100 dark:bg-gray-600 px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-500"
              >
                <Copy className="h-4 w-4" />
                <span className="text-sm">Copiar</span>
              </button>
            </div>
          </div>
        )}

        {/* Botões de Ação */}
        <div className="mt-4 flex gap-3 w-full">
          {boletoData.externalResourceUrl && (
            <button
              onClick={handleOpenBoleto}
              className="flex-1 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
            >
              Visualizar Boleto
            </button>
          )}
          <button
            onClick={onClose}
            className={`rounded-lg px-4 py-2 ${
              boletoData.externalResourceUrl
                ? 'flex-1 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700'
                : 'w-full bg-blue-600 text-white hover:bg-blue-700'
            }`}
          >
            Fechar
          </button>
        </div>

        {/* Aviso */}
        <div className="mt-4 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 p-3 text-center">
          <p className="text-xs text-amber-700 dark:text-amber-400">
            O pagamento será confirmado em até 3 dias úteis após o pagamento do boleto.
          </p>
        </div>
      </div>
    );
  }

  if (status === 'processing') {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <Loader2 className="h-12 w-12 animate-spin text-blue-500" />
        <p className="mt-4 text-gray-600 dark:text-gray-400">Processando pagamento...</p>
        <p className="mt-2 text-sm text-gray-500">Não feche esta janela</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="rounded-lg bg-gray-100 dark:bg-gray-700 p-4 text-center">
        <p className="text-sm text-gray-600 dark:text-gray-400">Valor a pagar</p>
        <p className="text-2xl font-bold text-gray-900 dark:text-white">
          {formatCurrency(valor)}
        </p>
      </div>

      {/* Mensagem de erro */}
      {status === 'error' && errorMessage && (
        <div className="flex items-center gap-2 rounded-lg bg-red-100 dark:bg-red-900/30 p-4 text-red-700 dark:text-red-400">
          <XCircle className="h-5 w-5 flex-shrink-0" />
          <p className="text-sm">{errorMessage}</p>
        </div>
      )}

      {/* Payment Brick do Mercado Pago */}
      <div className="min-h-[400px] rounded-lg overflow-hidden">
        {sdkInitialized && (
          <Payment
            key={theme} // Força re-render quando o tema muda
            initialization={initialization}
            customization={customization}
            onSubmit={handleOnSubmit}
            onReady={handleOnReady}
            onError={handleOnError}
          />
        )}
      </div>

      {/* Botão de fechar */}
      <div className="flex justify-center pt-4">
        <button
          onClick={onClose}
          className="text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
        >
          Cancelar e voltar
        </button>
      </div>
    </div>
  );
}
