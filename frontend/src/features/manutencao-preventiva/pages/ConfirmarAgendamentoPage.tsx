import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Check, X, Calendar, Clock, Car, Wrench, Loader2, AlertTriangle, CheckCircle, MapPin, Phone } from 'lucide-react';
import axios from 'axios';

interface AgendamentoData {
  status: string;
  mensagem: string | null;
  podeConfirmar: boolean;
  dataAgendamento: string;
  dataFormatada: string;
  horaAgendamento: string;
  horaFormatada: string;
  duracaoEstimadaMinutos: number;
  tipoManutencao: string;
  tipoManutencaoDescricao: string;
  descricao: string | null;
  observacoes: string | null;
  statusAgendamento: string;
  statusDescricao: string;
  veiculoPlaca: string;
  veiculoMarca: string;
  veiculoModelo: string;
  veiculoAno: number | null;
  oficinaNome: string;
  oficinaTelefone: string | null;
  oficinaEndereco: string | null;
}

interface ResultData {
  status: string;
  mensagem: string;
  dataAgendamento?: string;
  horaAgendamento?: string;
}

// API URL base
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export function ConfirmarAgendamentoPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [loading, setLoading] = useState(true);
  const [agendamento, setAgendamento] = useState<AgendamentoData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<ResultData | null>(null);
  const [motivoCancelamento, setMotivoCancelamento] = useState('');
  const [showCancelForm, setShowCancelForm] = useState(false);

  useEffect(() => {
    if (!token) {
      setError('Link de confirmacao invalido');
      setLoading(false);
      return;
    }

    const fetchAgendamento = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/api/public/agendamento/${token}`);

        if (response.data.status === 'JA_CONFIRMADO') {
          setResult({
            status: 'JA_CONFIRMADO',
            mensagem: response.data.mensagem,
          });
        } else if (response.data.status === 'ERRO') {
          setError(response.data.mensagem);
        } else {
          setAgendamento(response.data);
        }
      } catch (err: any) {
        if (err.response?.status === 404) {
          setError('Agendamento nao encontrado. O link pode ter expirado ou ser invalido.');
        } else if (err.response?.data?.mensagem) {
          setError(err.response.data.mensagem);
        } else {
          setError('Erro ao carregar agendamento. Tente novamente mais tarde.');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchAgendamento();
  }, [token]);

  const handleConfirmar = async () => {
    if (!token) return;

    setSubmitting(true);
    try {
      const response = await axios.post(`${API_BASE_URL}/api/public/agendamento/${token}/confirmar`);
      setResult(response.data);
    } catch (err: any) {
      if (err.response?.data?.mensagem) {
        setError(err.response.data.mensagem);
      } else {
        setError('Erro ao confirmar agendamento. Tente novamente.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancelar = async () => {
    if (!token) return;

    setSubmitting(true);
    try {
      const response = await axios.post(`${API_BASE_URL}/api/public/agendamento/${token}/rejeitar`, {
        motivo: motivoCancelamento,
      });
      setResult(response.data);
    } catch (err: any) {
      if (err.response?.data?.mensagem) {
        setError(err.response.data.mensagem);
      } else {
        setError('Erro ao cancelar agendamento. Tente novamente.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  // Loading state
  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-50">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="h-12 w-12 animate-spin text-blue-600" />
          <p className="text-gray-600">Carregando agendamento...</p>
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

  // Result state (confirmed, cancelled, or already confirmed)
  if (result) {
    const isSuccess = result.status === 'CONFIRMADO' || result.status === 'JA_CONFIRMADO';
    const isCancelled = result.status === 'CANCELADO';

    return (
      <div
        className={`flex min-h-screen items-center justify-center p-6 ${
          isSuccess
            ? 'bg-gradient-to-br from-green-50 to-emerald-50'
            : isCancelled
              ? 'bg-gradient-to-br from-gray-50 to-slate-50'
              : 'bg-gradient-to-br from-blue-50 to-indigo-50'
        }`}
      >
        <div className="w-full max-w-md">
          <div className="rounded-2xl bg-white p-8 shadow-xl">
            <div className="mb-6 flex justify-center">
              <div className={`rounded-full p-4 ${isSuccess ? 'bg-green-100' : 'bg-gray-100'}`}>
                {isSuccess ? (
                  <CheckCircle className="h-12 w-12 text-green-600" />
                ) : (
                  <X className="h-12 w-12 text-gray-600" />
                )}
              </div>
            </div>
            <div className="text-center">
              <h1 className="text-2xl font-bold text-gray-900">
                {isSuccess ? 'Agendamento Confirmado!' : 'Agendamento Cancelado'}
              </h1>
              <p className="mt-4 text-gray-600">{result.mensagem}</p>
              {result.dataAgendamento && result.horaAgendamento && (
                <div className="mt-4 p-4 bg-green-50 rounded-lg">
                  <p className="text-lg font-semibold text-green-800">
                    {result.dataAgendamento} as {result.horaAgendamento}
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Agendamento display
  if (!agendamento) return null;

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-50 p-4 sm:p-6">
      <div className="mx-auto max-w-2xl">
        {/* Header */}
        <div className="mb-6 text-center">
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900">Confirmar Agendamento</h1>
          <p className="mt-2 text-gray-600">Confirme ou cancele seu agendamento</p>
        </div>

        {/* Card Principal */}
        <div className="rounded-2xl bg-white p-4 sm:p-6 shadow-xl">
          {/* Status */}
          <div className="mb-6 flex flex-col gap-3 border-b border-gray-200 pb-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <Calendar className="h-8 w-8 text-blue-600 shrink-0" />
              <div>
                <p className="text-sm text-gray-500">Agendamento</p>
                <p className="text-xl sm:text-2xl font-bold text-gray-900">{agendamento.tipoManutencaoDescricao}</p>
              </div>
            </div>
            <span
              className={`self-start rounded-full px-4 py-2 text-sm font-medium sm:self-auto ${
                agendamento.statusAgendamento === 'AGENDADO'
                  ? 'bg-yellow-100 text-yellow-800'
                  : agendamento.statusAgendamento === 'CONFIRMADO'
                    ? 'bg-green-100 text-green-800'
                    : 'bg-gray-100 text-gray-800'
              }`}
            >
              {agendamento.statusDescricao}
            </span>
          </div>

          {/* Detalhes */}
          <div className="space-y-4">
            {/* Data e Hora */}
            <div className="grid grid-cols-2 gap-4">
              <div className="flex items-center gap-3 text-gray-600">
                <Calendar className="h-5 w-5 shrink-0" />
                <div>
                  <p className="text-xs text-gray-400">Data</p>
                  <p className="font-medium text-gray-900">{agendamento.dataFormatada}</p>
                </div>
              </div>
              <div className="flex items-center gap-3 text-gray-600">
                <Clock className="h-5 w-5 shrink-0" />
                <div>
                  <p className="text-xs text-gray-400">Horario</p>
                  <p className="font-medium text-gray-900">{agendamento.horaFormatada}</p>
                </div>
              </div>
            </div>

            {/* Veiculo */}
            <div className="rounded-lg bg-gray-50 p-4">
              <div className="flex items-center gap-3">
                <Car className="h-5 w-5 text-gray-500 shrink-0" />
                <div>
                  <p className="text-xs text-gray-400">Veiculo</p>
                  <p className="font-medium text-gray-900">
                    {agendamento.veiculoMarca} {agendamento.veiculoModelo}
                    {agendamento.veiculoAno && ` (${agendamento.veiculoAno})`}
                  </p>
                  <p className="text-sm text-gray-500">Placa: {agendamento.veiculoPlaca}</p>
                </div>
              </div>
            </div>

            {/* Servico */}
            <div className="rounded-lg bg-blue-50 p-4">
              <div className="flex items-start gap-3">
                <Wrench className="h-5 w-5 text-blue-500 shrink-0 mt-0.5" />
                <div>
                  <p className="text-xs text-blue-400">Servico</p>
                  <p className="font-medium text-blue-900">{agendamento.tipoManutencaoDescricao}</p>
                  {agendamento.descricao && (
                    <p className="mt-1 text-sm text-blue-700">{agendamento.descricao}</p>
                  )}
                </div>
              </div>
            </div>

            {/* Observacoes */}
            {agendamento.observacoes && (
              <div className="rounded-lg bg-amber-50 p-4">
                <p className="text-xs text-amber-600 mb-1">Observacoes</p>
                <p className="text-amber-800">{agendamento.observacoes}</p>
              </div>
            )}

            {/* Duracao */}
            {agendamento.duracaoEstimadaMinutos && (
              <div className="flex items-center gap-2 text-gray-500 text-sm">
                <Clock className="h-4 w-4" />
                <span>Duracao estimada: {agendamento.duracaoEstimadaMinutos} minutos</span>
              </div>
            )}
          </div>

          {/* Oficina */}
          <div className="mt-6 rounded-lg border border-gray-200 p-4">
            <h3 className="font-semibold text-gray-900 mb-3">{agendamento.oficinaNome}</h3>
            {agendamento.oficinaEndereco && (
              <div className="flex items-start gap-2 text-gray-600 text-sm mb-2">
                <MapPin className="h-4 w-4 shrink-0 mt-0.5" />
                <span>{agendamento.oficinaEndereco}</span>
              </div>
            )}
            {agendamento.oficinaTelefone && (
              <div className="flex items-center gap-2 text-gray-600 text-sm">
                <Phone className="h-4 w-4 shrink-0" />
                <a href={`tel:${agendamento.oficinaTelefone}`} className="text-blue-600 hover:underline">
                  {agendamento.oficinaTelefone}
                </a>
              </div>
            )}
          </div>

          {/* Botoes de acao */}
          {agendamento.podeConfirmar && !showCancelForm && (
            <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:gap-4">
              <button
                onClick={handleConfirmar}
                disabled={submitting}
                className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-green-600 px-6 py-3.5 font-medium text-white transition-colors hover:bg-green-700 disabled:opacity-50"
              >
                {submitting ? (
                  <Loader2 className="h-5 w-5 animate-spin" />
                ) : (
                  <Check className="h-5 w-5" />
                )}
                Confirmar Presenca
              </button>
              <button
                onClick={() => setShowCancelForm(true)}
                disabled={submitting}
                className="flex flex-1 items-center justify-center gap-2 rounded-lg border border-red-600 px-6 py-3.5 font-medium text-red-600 transition-colors hover:bg-red-50 disabled:opacity-50"
              >
                <X className="h-5 w-5" />
                Nao Posso Ir
              </button>
            </div>
          )}

          {/* Formulario de cancelamento */}
          {showCancelForm && (
            <div className="mt-6 rounded-lg border border-red-200 bg-red-50 p-4">
              <p className="mb-3 font-medium text-red-800">Tem certeza que nao pode comparecer?</p>
              <textarea
                value={motivoCancelamento}
                onChange={(e) => setMotivoCancelamento(e.target.value)}
                placeholder="Motivo (opcional) - Ex: conflito de horario, imprevisto..."
                className="mb-4 w-full rounded-lg border border-red-300 p-3 focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500/20"
                rows={3}
              />
              <div className="flex flex-col-reverse gap-3 sm:flex-row">
                <button
                  onClick={() => setShowCancelForm(false)}
                  disabled={submitting}
                  className="flex-1 rounded-lg border border-gray-300 px-4 py-2.5 font-medium text-gray-700 transition-colors hover:bg-gray-50 disabled:opacity-50"
                >
                  Voltar
                </button>
                <button
                  onClick={handleCancelar}
                  disabled={submitting}
                  className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-red-600 px-4 py-2.5 font-medium text-white transition-colors hover:bg-red-700 disabled:opacity-50"
                >
                  {submitting ? <Loader2 className="h-5 w-5 animate-spin" /> : <X className="h-5 w-5" />}
                  Confirmar Cancelamento
                </button>
              </div>
            </div>
          )}

          {/* Mensagem se nao pode confirmar */}
          {!agendamento.podeConfirmar && (
            <div className="mt-6 rounded-lg bg-yellow-50 p-4 text-center text-yellow-800">
              Este agendamento nao pode mais ser confirmado ou cancelado por aqui.
              <br />
              Entre em contato com a oficina.
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

export default ConfirmarAgendamentoPage;
