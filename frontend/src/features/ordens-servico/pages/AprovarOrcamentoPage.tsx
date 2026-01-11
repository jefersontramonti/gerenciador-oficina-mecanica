import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Check, X, FileText, Car, Clock, Loader2, AlertTriangle, CheckCircle } from 'lucide-react';
import axios from 'axios';

interface ItemOS {
  tipo: string;
  tipoDescricao: string;
  descricao: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
}

interface OrcamentoData {
  numero: number;
  status: string;
  statusDescricao: string;
  problemasRelatados: string;
  diagnostico: string;
  // Modelo híbrido de mão de obra
  tipoCobrancaMaoObra: 'VALOR_FIXO' | 'POR_HORA';
  valorMaoObra: number;
  tempoEstimadoHoras?: number;
  limiteHorasAprovado?: number;
  valorHoraSnapshot?: number;
  valorPecas: number;
  valorTotal: number;
  descontoPercentual: number;
  descontoValor: number;
  valorFinal: number;
  dataPrevisao: string | null;
  dataAbertura: string;
  podeAprovar: boolean;
  itens: ItemOS[];
}

interface AprovacaoResult {
  status: string;
  mensagem: string;
  numero?: number;
  valorFinal?: number;
}

// API URL base - usa variável de ambiente para funcionar em dev e produção
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export function AprovarOrcamentoPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [loading, setLoading] = useState(true);
  const [orcamento, setOrcamento] = useState<OrcamentoData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<AprovacaoResult | null>(null);
  const [motivoRejeicao, setMotivoRejeicao] = useState('');
  const [showRejeicaoForm, setShowRejeicaoForm] = useState(false);

  useEffect(() => {
    if (!token) {
      setError('Token de aprovação não encontrado');
      setLoading(false);
      return;
    }

    const fetchOrcamento = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/api/public/orcamento/${token}`);

        if (response.data.status === 'JA_APROVADO') {
          setResult({
            status: 'JA_APROVADO',
            mensagem: response.data.mensagem,
            numero: response.data.numero
          });
        } else if (response.data.erro) {
          setError(response.data.mensagem);
        } else {
          setOrcamento(response.data);
        }
      } catch (err: any) {
        if (err.response?.status === 404) {
          setError('Orçamento não encontrado. O link pode ter expirado ou ser inválido.');
        } else if (err.response?.data?.mensagem) {
          setError(err.response.data.mensagem);
        } else {
          setError('Erro ao carregar orçamento. Tente novamente mais tarde.');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchOrcamento();
  }, [token]);

  const handleAprovar = async () => {
    if (!token) return;

    setSubmitting(true);
    try {
      const response = await axios.post(`${API_BASE_URL}/api/public/orcamento/${token}/aprovar`);
      setResult(response.data);
    } catch (err: any) {
      if (err.response?.data?.mensagem) {
        setError(err.response.data.mensagem);
      } else {
        setError('Erro ao aprovar orçamento. Tente novamente.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleRejeitar = async () => {
    if (!token) return;

    setSubmitting(true);
    try {
      const response = await axios.post(`${API_BASE_URL}/api/public/orcamento/${token}/rejeitar`, {
        motivo: motivoRejeicao
      });
      setResult(response.data);
    } catch (err: any) {
      if (err.response?.data?.mensagem) {
        setError(err.response.data.mensagem);
      } else {
        setError('Erro ao rejeitar orçamento. Tente novamente.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(value);
  };


  // Loading state
  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-50">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="h-12 w-12 animate-spin text-blue-600" />
          <p className="text-gray-600">Carregando orcamento...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-red-50 to-orange-50 p-6">
        <div className="w-full max-w-md">
          <div className="rounded-2xl bg-white p-8 shadow-xl">
            <div className="mb-6 flex justify-center">
              <div className="rounded-full bg-red-100 p-4">
                <AlertTriangle className="h-12 w-12 text-red-600" />
              </div>
            </div>
            <div className="text-center">
              <h1 className="text-2xl font-bold text-gray-900">Erro</h1>
              <p className="mt-4 text-gray-600">{error}</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Result state (approved, rejected, or already approved)
  if (result) {
    const isSuccess = result.status === 'APROVADO' || result.status === 'JA_APROVADO';
    const isRejected = result.status === 'REJEITADO';

    return (
      <div className={`flex min-h-screen items-center justify-center p-6 ${
        isSuccess ? 'bg-gradient-to-br from-green-50 to-emerald-50' :
        isRejected ? 'bg-gradient-to-br from-gray-50 to-slate-50' :
        'bg-gradient-to-br from-blue-50 to-indigo-50'
      }`}>
        <div className="w-full max-w-md">
          <div className="rounded-2xl bg-white p-8 shadow-xl">
            <div className="mb-6 flex justify-center">
              <div className={`rounded-full p-4 ${
                isSuccess ? 'bg-green-100' : 'bg-gray-100'
              }`}>
                {isSuccess ? (
                  <CheckCircle className="h-12 w-12 text-green-600" />
                ) : (
                  <X className="h-12 w-12 text-gray-600" />
                )}
              </div>
            </div>
            <div className="text-center">
              <h1 className="text-2xl font-bold text-gray-900">
                {isSuccess ? 'Orcamento Aprovado!' : 'Orcamento Rejeitado'}
              </h1>
              <p className="mt-4 text-gray-600">{result.mensagem}</p>
              {result.numero && (
                <p className="mt-2 text-lg font-semibold text-gray-800">
                  OS #{result.numero}
                </p>
              )}
              {result.valorFinal && (
                <p className="mt-1 text-xl font-bold text-green-600">
                  {formatCurrency(result.valorFinal)}
                </p>
              )}
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Orcamento display
  if (!orcamento) return null;

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-50 p-6">
      <div className="mx-auto max-w-2xl">
        {/* Header */}
        <div className="mb-6 text-center">
          <h1 className="text-3xl font-bold text-gray-900">PitStop Pro</h1>
          <p className="mt-2 text-gray-600">Aprovacao de Orcamento</p>
        </div>

        {/* Card Principal */}
        <div className="rounded-2xl bg-white p-6 shadow-xl">
          {/* Numero da OS */}
          <div className="mb-6 flex flex-col gap-3 border-b border-gray-200 pb-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <FileText className="h-8 w-8 text-blue-600 shrink-0" />
              <div>
                <p className="text-sm text-gray-500">Ordem de Servico</p>
                <p className="text-xl sm:text-2xl font-bold text-gray-900">#{orcamento.numero}</p>
              </div>
            </div>
            <span className="self-start rounded-full bg-yellow-100 px-4 py-2 text-sm font-medium text-yellow-800 sm:self-auto">
              {orcamento.statusDescricao}
            </span>
          </div>

          {/* Detalhes */}
          <div className="space-y-4">
            {/* Data de abertura */}
            <div className="flex items-center gap-3 text-gray-600">
              <Clock className="h-5 w-5" />
              <span>Aberto em: {orcamento.dataAbertura}</span>
            </div>

            {/* Data de previsao */}
            {orcamento.dataPrevisao && (
              <div className="flex items-center gap-3 text-gray-600">
                <Car className="h-5 w-5" />
                <span>Previsao de conclusao: {orcamento.dataPrevisao}</span>
              </div>
            )}

            {/* Problemas relatados */}
            {orcamento.problemasRelatados && (
              <div className="rounded-lg bg-gray-50 p-4">
                <p className="text-sm font-medium text-gray-700">Problemas Relatados:</p>
                <p className="mt-1 text-gray-600">{orcamento.problemasRelatados}</p>
              </div>
            )}

            {/* Diagnostico */}
            {orcamento.diagnostico && (
              <div className="rounded-lg bg-blue-50 p-4">
                <p className="text-sm font-medium text-blue-700">Diagnostico:</p>
                <p className="mt-1 text-blue-600">{orcamento.diagnostico}</p>
              </div>
            )}
          </div>

          {/* Itens (Pecas e Servicos) */}
          {orcamento.itens && orcamento.itens.length > 0 && (
            <div className="mt-6 rounded-lg bg-gray-50 p-4">
              <h3 className="mb-3 text-lg font-semibold text-gray-900">Itens do Orcamento</h3>
              <div className="space-y-3">
                {orcamento.itens.map((item, index) => (
                  <div key={index} className="border-b border-gray-200 pb-3 last:border-0 last:pb-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className={`rounded px-2 py-0.5 text-xs font-medium ${
                        item.tipo === 'PECA'
                          ? 'bg-blue-100 text-blue-700'
                          : 'bg-purple-100 text-purple-700'
                      }`}>
                        {item.tipoDescricao}
                      </span>
                      <span className="font-medium text-gray-900 break-words">{item.descricao}</span>
                    </div>
                    <div className="mt-2 flex items-center justify-between text-sm">
                      <span className="text-gray-500">
                        {item.quantidade}x {formatCurrency(item.valorUnitario)}
                      </span>
                      <span className="font-medium text-gray-900">
                        {formatCurrency(item.valorTotal)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Valores */}
          <div className="mt-6 rounded-lg bg-gray-50 p-4">
            <h3 className="mb-3 text-lg font-semibold text-gray-900">Resumo</h3>
            <div className="space-y-2">
              {/* Mão de obra - Modelo Híbrido */}
              {orcamento.tipoCobrancaMaoObra === 'POR_HORA' ? (
                <div className="rounded-lg bg-blue-50 border border-blue-200 p-3 mb-3">
                  <div className="flex items-center gap-2 mb-2">
                    <Clock className="h-4 w-4 text-blue-600" />
                    <span className="text-sm font-medium text-blue-900">Mao de Obra por Hora</span>
                  </div>
                  <div className="text-sm text-blue-800 space-y-1">
                    <div className="flex justify-between">
                      <span>Valor/hora:</span>
                      <span className="font-medium">{formatCurrency(orcamento.valorHoraSnapshot || 0)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Tempo estimado:</span>
                      <span className="font-medium">{orcamento.tempoEstimadoHoras}h</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Estimativa:</span>
                      <span className="font-medium">
                        {formatCurrency((orcamento.tempoEstimadoHoras || 0) * (orcamento.valorHoraSnapshot || 0))}
                      </span>
                    </div>
                  </div>
                  <div className="mt-3 pt-3 border-t border-blue-200">
                    <div className="bg-orange-50 border border-orange-200 rounded p-2">
                      <p className="text-xs font-medium text-orange-800">
                        Limite que voce esta aprovando:
                      </p>
                      <p className="text-sm font-bold text-orange-900">
                        Ate {orcamento.limiteHorasAprovado}h = {formatCurrency((orcamento.limiteHorasAprovado || 0) * (orcamento.valorHoraSnapshot || 0))}
                      </p>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex justify-between">
                  <span className="text-gray-600">Mao de Obra:</span>
                  <span className="font-medium text-gray-900">{formatCurrency(orcamento.valorMaoObra)}</span>
                </div>
              )}
              <div className="flex justify-between">
                <span className="text-gray-600">Pecas:</span>
                <span className="font-medium text-gray-900">{formatCurrency(orcamento.valorPecas)}</span>
              </div>
              <div className="flex justify-between border-t border-gray-200 pt-2">
                <span className="text-gray-600">Subtotal:</span>
                <span className="font-medium text-gray-900">{formatCurrency(orcamento.valorTotal)}</span>
              </div>
              {(orcamento.descontoPercentual > 0 || orcamento.descontoValor > 0) && (
                <div className="flex justify-between">
                  <span className="text-green-700">Desconto:</span>
                  <span className="font-medium text-green-700">
                    -{formatCurrency(orcamento.descontoValor || (orcamento.valorTotal * orcamento.descontoPercentual / 100))}
                  </span>
                </div>
              )}
              <div className="flex justify-between border-t border-gray-300 pt-2">
                <span className="text-lg font-bold text-gray-900">Total:</span>
                <span className="text-lg font-bold text-green-600">
                  {formatCurrency(orcamento.valorFinal)}
                </span>
              </div>
            </div>
          </div>

          {/* Botoes de acao */}
          {orcamento.podeAprovar && !showRejeicaoForm && (
            <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:gap-4">
              <button
                onClick={handleAprovar}
                disabled={submitting}
                className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-green-600 px-6 py-3.5 font-medium text-white transition-colors hover:bg-green-700 disabled:opacity-50"
              >
                {submitting ? (
                  <Loader2 className="h-5 w-5 animate-spin" />
                ) : (
                  <Check className="h-5 w-5" />
                )}
                Aprovar Orcamento
              </button>
              <button
                onClick={() => setShowRejeicaoForm(true)}
                disabled={submitting}
                className="flex flex-1 items-center justify-center gap-2 rounded-lg border border-red-600 px-6 py-3.5 font-medium text-red-600 transition-colors hover:bg-red-50 disabled:opacity-50"
              >
                <X className="h-5 w-5" />
                Rejeitar
              </button>
            </div>
          )}

          {/* Formulario de rejeicao */}
          {showRejeicaoForm && (
            <div className="mt-6 rounded-lg border border-red-200 bg-red-50 p-4">
              <p className="mb-3 font-medium text-red-800">
                Tem certeza que deseja rejeitar o orcamento?
              </p>
              <textarea
                value={motivoRejeicao}
                onChange={(e) => setMotivoRejeicao(e.target.value)}
                placeholder="Motivo da rejeicao (opcional)"
                className="mb-4 w-full rounded-lg border border-red-300 p-3 focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500/20"
                rows={3}
              />
              <div className="flex flex-col-reverse gap-3 sm:flex-row">
                <button
                  onClick={() => setShowRejeicaoForm(false)}
                  disabled={submitting}
                  className="flex-1 rounded-lg border border-gray-300 px-4 py-2.5 font-medium text-gray-700 transition-colors hover:bg-gray-50 disabled:opacity-50"
                >
                  Cancelar
                </button>
                <button
                  onClick={handleRejeitar}
                  disabled={submitting}
                  className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-red-600 px-4 py-2.5 font-medium text-white transition-colors hover:bg-red-700 disabled:opacity-50"
                >
                  {submitting ? (
                    <Loader2 className="h-5 w-5 animate-spin" />
                  ) : (
                    <X className="h-5 w-5" />
                  )}
                  Confirmar Rejeicao
                </button>
              </div>
            </div>
          )}

          {/* Mensagem se nao pode aprovar */}
          {!orcamento.podeAprovar && (
            <div className="mt-6 rounded-lg bg-yellow-50 p-4 text-center text-yellow-800">
              Este orcamento nao pode mais ser aprovado ou rejeitado.
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="mt-6 text-center text-sm text-gray-500">
          <p>Em caso de duvidas, entre em contato com a oficina.</p>
        </div>
      </div>
    </div>
  );
}
