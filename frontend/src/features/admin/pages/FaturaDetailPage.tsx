import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  FileText,
  Building2,
  Calendar,
  DollarSign,
  CreditCard,
  X,
  CheckCircle,
  AlertTriangle,
  Clock,
  Copy,
  ExternalLink,
} from 'lucide-react';
import { useFaturaDetail, useCancelarFatura } from '../hooks/useSaas';
import type { StatusFatura } from '../types';
import { RegistrarPagamentoModal } from '../components/RegistrarPagamentoModal';

const statusColors: Record<StatusFatura, string> = {
  PENDENTE: 'bg-yellow-100 text-yellow-800 border-yellow-200 dark:bg-yellow-900/30 dark:text-yellow-400 dark:border-yellow-800',
  PAGO: 'bg-green-100 text-green-800 border-green-200 dark:bg-green-900/30 dark:text-green-400 dark:border-green-800',
  VENCIDO: 'bg-red-100 text-red-800 border-red-200 dark:bg-red-900/30 dark:text-red-400 dark:border-red-800',
  CANCELADO: 'bg-gray-100 text-gray-800 border-gray-200 dark:bg-gray-700 dark:text-gray-300 dark:border-gray-600',
};

const statusIcons: Record<StatusFatura, React.ReactNode> = {
  PENDENTE: <Clock className="h-5 w-5" />,
  PAGO: <CheckCircle className="h-5 w-5" />,
  VENCIDO: <AlertTriangle className="h-5 w-5" />,
  CANCELADO: <X className="h-5 w-5" />,
};

export function FaturaDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [showPaymentModal, setShowPaymentModal] = useState(false);

  const { data: fatura, isLoading, error } = useFaturaDetail(id);
  const cancelarFatura = useCancelarFatura();

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  };

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('pt-BR');
  };

  const formatDateTime = (date: string) => {
    return new Date(date).toLocaleString('pt-BR');
  };

  const handleCancelar = async () => {
    if (!fatura) return;
    const motivo = prompt('Motivo do cancelamento:');
    if (motivo) {
      await cancelarFatura.mutateAsync({ id: fatura.id, motivo });
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    alert('Copiado para a área de transferência!');
  };

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 border-t-transparent" />
      </div>
    );
  }

  if (error || !fatura) {
    return (
      <div className="m-6 rounded-lg border border-red-200 bg-red-50 p-6 text-center dark:border-red-800 dark:bg-red-900/20">
        <AlertTriangle className="mx-auto h-12 w-12 text-red-500 dark:text-red-400" />
        <h2 className="mt-4 text-lg font-semibold text-red-800 dark:text-red-300">Fatura não encontrada</h2>
        <p className="mt-2 text-red-600 dark:text-red-400">A fatura solicitada não existe ou foi removida.</p>
        <button
          onClick={() => navigate('/admin/faturas')}
          className="mt-4 rounded-lg bg-red-600 px-4 py-2 text-white hover:bg-red-700 dark:bg-red-500 dark:hover:bg-red-600"
        >
          Voltar para Faturas
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/admin/faturas')}
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-700"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <div className="flex items-center gap-3">
              <FileText className="h-6 w-6 text-blue-600 dark:text-blue-400" />
              <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Fatura {fatura.numero}</h1>
              <span
                className={`inline-flex items-center gap-1 rounded-full border px-3 py-1 text-sm font-medium ${statusColors[fatura.status]}`}
              >
                {statusIcons[fatura.status]}
                {fatura.statusLabel}
              </span>
            </div>
            <p className="mt-1 text-gray-600 dark:text-gray-400">
              Referência: {fatura.mesReferenciaFormatado}
            </p>
          </div>
        </div>

        <div className="flex gap-3">
          {fatura.pagavel && (
            <button
              onClick={() => setShowPaymentModal(true)}
              className="flex items-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-white hover:bg-green-700 dark:bg-green-500 dark:hover:bg-green-600"
            >
              <DollarSign className="h-4 w-4" />
              Registrar Pagamento
            </button>
          )}
          {fatura.cancelavel && (
            <button
              onClick={handleCancelar}
              disabled={cancelarFatura.isPending}
              className="flex items-center gap-2 rounded-lg border border-red-600 px-4 py-2 text-red-600 hover:bg-red-50 disabled:opacity-50 dark:border-red-500 dark:text-red-400 dark:hover:bg-red-900/20"
            >
              <X className="h-4 w-4" />
              Cancelar Fatura
            </button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Main Content */}
        <div className="space-y-6 lg:col-span-2">
          {/* Invoice Info */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">Informações da Fatura</h2>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Número</p>
                <p className="font-mono font-medium text-gray-900 dark:text-white">{fatura.numero}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Mês de Referência</p>
                <p className="font-medium text-gray-900 dark:text-white">{fatura.mesReferenciaFormatado}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Data de Emissão</p>
                <p className="font-medium text-gray-900 dark:text-white">{formatDate(fatura.dataEmissao)}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Data de Vencimento</p>
                <p className={`font-medium ${fatura.vencida ? 'text-red-600 dark:text-red-400' : 'text-gray-900 dark:text-white'}`}>
                  {formatDate(fatura.dataVencimento)}
                  {fatura.diasAteVencimento < 0 && (
                    <span className="ml-2 text-sm text-red-500 dark:text-red-400">
                      ({Math.abs(fatura.diasAteVencimento)} dias atrás)
                    </span>
                  )}
                  {fatura.diasAteVencimento > 0 && fatura.diasAteVencimento <= 5 && (
                    <span className="ml-2 text-sm text-yellow-600 dark:text-yellow-400">
                      (vence em {fatura.diasAteVencimento} dias)
                    </span>
                  )}
                </p>
              </div>
              {fatura.dataPagamento && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Data de Pagamento</p>
                  <p className="font-medium text-green-600 dark:text-green-400">{formatDate(fatura.dataPagamento)}</p>
                </div>
              )}
              {fatura.metodoPagamento && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Método de Pagamento</p>
                  <p className="font-medium text-gray-900 dark:text-white">{fatura.metodoPagamento}</p>
                </div>
              )}
            </div>
          </div>

          {/* Items */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">Itens da Fatura</h2>
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-700">
                  <th className="pb-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Descrição</th>
                  <th className="pb-3 text-center text-sm font-medium text-gray-500 dark:text-gray-400">Qtd</th>
                  <th className="pb-3 text-right text-sm font-medium text-gray-500 dark:text-gray-400">Valor Unit.</th>
                  <th className="pb-3 text-right text-sm font-medium text-gray-500 dark:text-gray-400">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                {fatura.itens.map((item) => (
                  <tr key={item.id}>
                    <td className="py-3 text-sm text-gray-900 dark:text-gray-100">{item.descricao}</td>
                    <td className="py-3 text-center text-sm text-gray-600 dark:text-gray-400">{item.quantidade}</td>
                    <td className="py-3 text-right text-sm text-gray-600 dark:text-gray-400">
                      {formatCurrency(item.valorUnitario)}
                    </td>
                    <td className="py-3 text-right text-sm font-medium text-gray-900 dark:text-gray-100">
                      {formatCurrency(item.valorTotal)}
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot className="border-t border-gray-200 dark:border-gray-700">
                <tr>
                  <td colSpan={3} className="pt-3 text-right text-sm text-gray-500 dark:text-gray-400">
                    Subtotal
                  </td>
                  <td className="pt-3 text-right text-sm text-gray-900 dark:text-gray-100">
                    {formatCurrency(fatura.valorBase)}
                  </td>
                </tr>
                {fatura.valorDesconto > 0 && (
                  <tr>
                    <td colSpan={3} className="pt-1 text-right text-sm text-green-600 dark:text-green-400">
                      Desconto
                    </td>
                    <td className="pt-1 text-right text-sm text-green-600 dark:text-green-400">
                      -{formatCurrency(fatura.valorDesconto)}
                    </td>
                  </tr>
                )}
                {fatura.valorAcrescimos > 0 && (
                  <tr>
                    <td colSpan={3} className="pt-1 text-right text-sm text-red-600 dark:text-red-400">
                      Acréscimos
                    </td>
                    <td className="pt-1 text-right text-sm text-red-600 dark:text-red-400">
                      +{formatCurrency(fatura.valorAcrescimos)}
                    </td>
                  </tr>
                )}
                <tr>
                  <td colSpan={3} className="pt-3 text-right text-lg font-semibold text-gray-900 dark:text-white">
                    Total
                  </td>
                  <td className="pt-3 text-right text-lg font-bold text-gray-900 dark:text-white">
                    {formatCurrency(fatura.valorTotal)}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>

          {/* Payment Info */}
          {(fatura.qrCodePix || fatura.linkPagamento || fatura.transacaoId) && (
            <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
              <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">Informações de Pagamento</h2>
              <div className="space-y-4">
                {fatura.transacaoId && (
                  <div className="flex items-center justify-between rounded-lg bg-gray-50 p-3 dark:bg-gray-700">
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">ID da Transação</p>
                      <p className="font-mono text-sm text-gray-900 dark:text-gray-100">{fatura.transacaoId}</p>
                    </div>
                    <button
                      onClick={() => copyToClipboard(fatura.transacaoId!)}
                      className="rounded p-2 text-gray-500 hover:bg-gray-200 dark:text-gray-400 dark:hover:bg-gray-600"
                    >
                      <Copy className="h-4 w-4" />
                    </button>
                  </div>
                )}
                {fatura.linkPagamento && (
                  <div className="flex items-center justify-between rounded-lg bg-blue-50 p-3 dark:bg-blue-900/20">
                    <div>
                      <p className="text-sm text-blue-600 dark:text-blue-400">Link de Pagamento</p>
                      <p className="text-sm text-blue-800 truncate max-w-md dark:text-blue-300">{fatura.linkPagamento}</p>
                    </div>
                    <a
                      href={fatura.linkPagamento}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="rounded p-2 text-blue-600 hover:bg-blue-100 dark:text-blue-400 dark:hover:bg-blue-900/40"
                    >
                      <ExternalLink className="h-4 w-4" />
                    </a>
                  </div>
                )}
                {fatura.qrCodePix && (
                  <div className="rounded-lg bg-gray-50 p-4 text-center dark:bg-gray-700">
                    <p className="mb-2 text-sm text-gray-500 dark:text-gray-400">QR Code PIX</p>
                    <div className="mx-auto flex h-48 w-48 items-center justify-center rounded-lg border-2 border-dashed border-gray-300 bg-white dark:border-gray-600 dark:bg-gray-800">
                      <img
                        src={`data:image/png;base64,${fatura.qrCodePix}`}
                        alt="QR Code PIX"
                        className="h-40 w-40"
                      />
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Observations */}
          {fatura.observacao && (
            <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
              <h2 className="mb-2 text-lg font-semibold text-gray-900 dark:text-white">Observações</h2>
              <p className="text-gray-600 whitespace-pre-wrap dark:text-gray-400">{fatura.observacao}</p>
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Workshop Info */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <div className="mb-4 flex items-center gap-2">
              <Building2 className="h-5 w-5 text-gray-400 dark:text-gray-500" />
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Oficina</h2>
            </div>
            <div className="space-y-3">
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Nome</p>
                <Link
                  to={`/admin/oficinas/${fatura.oficinaId}`}
                  className="font-medium text-blue-600 hover:underline dark:text-blue-400"
                >
                  {fatura.oficinaNome}
                </Link>
              </div>
              {fatura.oficinaCnpj && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">CNPJ</p>
                  <p className="font-medium text-gray-900 dark:text-white">{fatura.oficinaCnpj}</p>
                </div>
              )}
              {fatura.oficinaEmail && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Email</p>
                  <p className="text-sm text-gray-600 dark:text-gray-400">{fatura.oficinaEmail}</p>
                </div>
              )}
              {fatura.planoCodigo && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Plano</p>
                  <p className="font-medium text-gray-900 dark:text-white">{fatura.planoCodigo}</p>
                </div>
              )}
            </div>
          </div>

          {/* Summary */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <div className="mb-4 flex items-center gap-2">
              <CreditCard className="h-5 w-5 text-gray-400 dark:text-gray-500" />
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Resumo</h2>
            </div>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-gray-500 dark:text-gray-400">Valor Total</span>
                <span className="text-xl font-bold text-gray-900 dark:text-white">{formatCurrency(fatura.valorTotal)}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-gray-500 dark:text-gray-400">Status</span>
                <span
                  className={`inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-sm font-medium ${statusColors[fatura.status]}`}
                >
                  {fatura.statusLabel}
                </span>
              </div>
              {fatura.tentativasCobranca > 0 && (
                <div className="flex items-center justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Tentativas de Cobrança</span>
                  <span className="text-gray-900 dark:text-white">{fatura.tentativasCobranca}</span>
                </div>
              )}
            </div>
          </div>

          {/* Timeline */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <div className="mb-4 flex items-center gap-2">
              <Calendar className="h-5 w-5 text-gray-400 dark:text-gray-500" />
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Histórico</h2>
            </div>
            <div className="space-y-4">
              <div className="flex gap-3">
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400">
                  <FileText className="h-4 w-4" />
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-900 dark:text-white">Fatura Criada</p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">{formatDateTime(fatura.createdAt)}</p>
                </div>
              </div>
              {fatura.dataPagamento && (
                <div className="flex gap-3">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400">
                    <CheckCircle className="h-4 w-4" />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-gray-900 dark:text-white">Pagamento Registrado</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">{formatDate(fatura.dataPagamento)}</p>
                  </div>
                </div>
              )}
              {fatura.status === 'CANCELADO' && (
                <div className="flex gap-3">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400">
                    <X className="h-4 w-4" />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-gray-900 dark:text-white">Fatura Cancelada</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">{formatDateTime(fatura.updatedAt)}</p>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Payment Modal */}
      {showPaymentModal && (
        <RegistrarPagamentoModal
          fatura={fatura}
          onClose={() => setShowPaymentModal(false)}
          onSuccess={() => setShowPaymentModal(false)}
        />
      )}
    </div>
  );
}
