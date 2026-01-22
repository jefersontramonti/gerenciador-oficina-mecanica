import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, FileText, Building2, Calendar, Loader2 } from 'lucide-react';
import { useOficinas, useGerarFaturaParaOficina } from '../hooks/useSaas';

/**
 * Page for generating a new invoice for a specific workshop.
 * Allows SUPER_ADMIN to select an oficina and generate a monthly invoice.
 */
export function FaturaFormPage() {
  const navigate = useNavigate();
  const [selectedOficinaId, setSelectedOficinaId] = useState('');
  const [mesReferencia, setMesReferencia] = useState(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
  });

  const { data: oficinasData, isLoading: isLoadingOficinas } = useOficinas({ size: 100 });
  const gerarFatura = useGerarFaturaParaOficina();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!selectedOficinaId) {
      alert('Selecione uma oficina');
      return;
    }

    try {
      const fatura = await gerarFatura.mutateAsync({
        oficinaId: selectedOficinaId,
        mesReferencia,
      });
      navigate(`/admin/faturas/${fatura.id}`);
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao gerar fatura');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/admin/faturas')}
          className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
        >
          <ArrowLeft className="h-5 w-5 text-gray-600 dark:text-gray-400" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Nova Fatura
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Gerar fatura manualmente para uma oficina
          </p>
        </div>
      </div>

      {/* Form */}
      <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Oficina Selection */}
          <div>
            <label className="mb-2 flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
              <Building2 className="h-4 w-4" />
              Oficina
            </label>
            <select
              value={selectedOficinaId}
              onChange={(e) => setSelectedOficinaId(e.target.value)}
              className="w-full rounded-lg border border-gray-300 bg-white px-4 py-2 text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              required
            >
              <option value="">Selecione uma oficina...</option>
              {isLoadingOficinas ? (
                <option disabled>Carregando...</option>
              ) : (
                oficinasData?.content.map((oficina) => (
                  <option key={oficina.id} value={oficina.id}>
                    {oficina.nomeFantasia} - {oficina.cnpjCpf}
                  </option>
                ))
              )}
            </select>
          </div>

          {/* Mês de Referência */}
          <div>
            <label className="mb-2 flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
              <Calendar className="h-4 w-4" />
              Mês de Referência
            </label>
            <input
              type="date"
              value={mesReferencia}
              onChange={(e) => setMesReferencia(e.target.value)}
              className="w-full rounded-lg border border-gray-300 bg-white px-4 py-2 text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              required
            />
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              O sistema usará o primeiro dia do mês selecionado como referência.
            </p>
          </div>

          {/* Info Box */}
          <div className="rounded-lg bg-blue-50 p-4 dark:bg-blue-900/20">
            <div className="flex items-start gap-3">
              <FileText className="h-5 w-5 text-blue-600 dark:text-blue-400" />
              <div>
                <h4 className="font-medium text-blue-800 dark:text-blue-300">
                  Como funciona
                </h4>
                <p className="mt-1 text-sm text-blue-700 dark:text-blue-400">
                  A fatura será gerada automaticamente com o valor da mensalidade do plano
                  da oficina. O vencimento será definido para 10 dias após a data de emissão.
                </p>
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-3 border-t border-gray-200 pt-6 dark:border-gray-700">
            <button
              type="button"
              onClick={() => navigate('/admin/faturas')}
              className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={gerarFatura.isPending || !selectedOficinaId}
              className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-blue-500 dark:hover:bg-blue-600"
            >
              {gerarFatura.isPending ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Gerando...
                </>
              ) : (
                <>
                  <FileText className="h-4 w-4" />
                  Gerar Fatura
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
