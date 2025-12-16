/**
 * Página de detalhes da Ordem de Serviço
 * Exibe informações completas, timeline, itens e ações disponíveis
 */

import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Edit, Car, User, Phone, Mail, FileText, FileDown, DollarSign } from 'lucide-react';
import { showError } from '@/shared/utils/notifications';
import { useOrdemServico, useGerarPDF } from '../hooks/useOrdensServico';
import { StatusBadge } from '../components/StatusBadge';
import { StatusTimeline } from '../components/StatusTimeline';
import { ItemOSTable } from '../components/ItemOSTable';
import { ActionButtons } from '../components/ActionButtons';
import { canEdit } from '../utils/statusTransitions';
import { ResumoFinanceiro } from '@/features/financeiro/components/ResumoFinanceiro';
import { ListaPagamentos } from '@/features/financeiro/components/ListaPagamentos';
import { PagamentoModal } from '@/features/financeiro/components/PagamentoModal';
import { useResumoFinanceiro } from '@/features/financeiro/hooks/usePagamentos';
import { useState } from 'react';

/**
 * Formata valor monetário
 */
const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

export const OrdemServicoDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const { data: ordemServico, isLoading, error, refetch } = useOrdemServico(id);
  const { data: resumoFinanceiro } = useResumoFinanceiro(id || '');
  const gerarPDFMutation = useGerarPDF();
  const [mostrarModalPagamento, setMostrarModalPagamento] = useState(false);

  const handleGerarPDF = async () => {
    if (!ordemServico) return;

    try {
      const blob = await gerarPDFMutation.mutateAsync(ordemServico.id);

      // Criar URL temporária para o blob
      const url = window.URL.createObjectURL(blob);

      // Criar link temporário e simular click para download
      const link = document.createElement('a');
      link.href = url;
      link.download = `OS-${ordemServico.numero}.pdf`;
      document.body.appendChild(link);
      link.click();

      // Limpar
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'Erro ao gerar PDF';
      showError(`Erro: ${errorMessage}`);
    }
  };

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <p className="text-gray-600">Carregando...</p>
      </div>
    );
  }

  if (error || !ordemServico) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Erro ao carregar ordem de serviço. Tente novamente.
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6">
        <div className="mb-4 flex items-center gap-4">
          <Link
            to="/ordens-servico"
            className="rounded-lg border border-gray-300 p-2 text-gray-700 hover:bg-gray-50"
          >
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div className="flex-1">
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-gray-900">OS #{ordemServico.numero}</h1>
              <StatusBadge status={ordemServico.status} />
            </div>
            <p className="mt-1 text-sm text-gray-600">
              Detalhes completos da ordem de serviço
            </p>
          </div>

          {/* Botões de Ação */}
          <div className="flex items-center gap-3">
            {/* Botão Imprimir PDF */}
            <button
              type="button"
              onClick={handleGerarPDF}
              disabled={gerarPDFMutation.isPending}
              className="flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 disabled:opacity-50"
            >
              <FileDown className="h-5 w-5" />
              {gerarPDFMutation.isPending ? 'Gerando...' : 'Imprimir PDF'}
            </button>

            {/* Botão Editar */}
            {canEdit(ordemServico.status) && (
              <Link
                to={`/ordens-servico/${ordemServico.id}/editar`}
                className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
              >
                <Edit className="h-5 w-5" />
                Editar
              </Link>
            )}
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Coluna Principal (2/3) */}
        <div className="space-y-6 lg:col-span-2">
          {/* Seção: Informações do Veículo e Cliente */}
          <div className="rounded-lg bg-white p-6 shadow">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900">
              <Car className="h-5 w-5" />
              Informações do Veículo e Cliente
            </h2>

            <div className="space-y-4">
              {/* Veículo */}
              {ordemServico.veiculo && (
                <div className="rounded-lg border border-gray-200 bg-gray-50 p-4">
                  <h3 className="mb-2 text-sm font-medium text-gray-700">Veículo</h3>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <span className="text-xs text-gray-500">Placa:</span>
                      <p className="font-medium text-gray-900">{ordemServico.veiculo.placa}</p>
                    </div>
                    <div>
                      <span className="text-xs text-gray-500">Marca/Modelo:</span>
                      <p className="font-medium text-gray-900">
                        {ordemServico.veiculo.marca} {ordemServico.veiculo.modelo}
                      </p>
                    </div>
                    <div>
                      <span className="text-xs text-gray-500">Ano:</span>
                      <p className="font-medium text-gray-900">{ordemServico.veiculo.ano}</p>
                    </div>
                    {ordemServico.veiculo.cor && (
                      <div>
                        <span className="text-xs text-gray-500">Cor:</span>
                        <p className="font-medium text-gray-900">{ordemServico.veiculo.cor}</p>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Cliente */}
              {ordemServico.cliente && (
                <div className="rounded-lg border border-gray-200 bg-gray-50 p-4">
                  <h3 className="mb-2 flex items-center gap-2 text-sm font-medium text-gray-700">
                    <User className="h-4 w-4" />
                    Cliente
                  </h3>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <span className="text-xs text-gray-500">Nome:</span>
                      <p className="font-medium text-gray-900">
                        {ordemServico.cliente.nome}
                      </p>
                    </div>
                    <div>
                      <span className="text-xs text-gray-500">CPF/CNPJ:</span>
                      <p className="font-medium text-gray-900">
                        {ordemServico.cliente.cpfCnpj}
                      </p>
                    </div>
                    {ordemServico.cliente.telefone && (
                      <div className="flex items-center gap-2">
                        <Phone className="h-4 w-4 text-gray-400" />
                        <span className="text-sm text-gray-700">
                          {ordemServico.cliente.telefone}
                        </span>
                      </div>
                    )}
                    {ordemServico.cliente.celular && (
                      <div className="flex items-center gap-2">
                        <Phone className="h-4 w-4 text-gray-400" />
                        <span className="text-sm text-gray-700">
                          {ordemServico.cliente.celular}
                        </span>
                      </div>
                    )}
                    {ordemServico.cliente.email && (
                      <div className="flex items-center gap-2">
                        <Mail className="h-4 w-4 text-gray-400" />
                        <span className="text-sm text-gray-700">
                          {ordemServico.cliente.email}
                        </span>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {!ordemServico.veiculo && !ordemServico.cliente && (
                <p className="text-sm text-gray-500">Informações não disponíveis</p>
              )}
            </div>
          </div>

          {/* Seção: Informações da OS */}
          <div className="rounded-lg bg-white p-6 shadow">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900">
              <FileText className="h-5 w-5" />
              Informações da OS
            </h2>

            <div className="space-y-4">
              {/* Mecânico */}
              {ordemServico.mecanico && (
                <div>
                  <span className="text-sm font-medium text-gray-700">Mecânico Responsável:</span>
                  <p className="mt-1 text-gray-900">{ordemServico.mecanico.nome}</p>
                  <p className="text-sm text-gray-500">{ordemServico.mecanico.email}</p>
                </div>
              )}

              {/* Problemas Relatados */}
              <div>
                <span className="text-sm font-medium text-gray-700">Problemas Relatados:</span>
                <p className="mt-1 whitespace-pre-wrap rounded-lg bg-gray-50 p-3 text-gray-900">
                  {ordemServico.problemasRelatados}
                </p>
              </div>

              {/* Diagnóstico */}
              {ordemServico.diagnostico && (
                <div>
                  <span className="text-sm font-medium text-gray-700">Diagnóstico:</span>
                  <p className="mt-1 whitespace-pre-wrap rounded-lg bg-blue-50 p-3 text-gray-900">
                    {ordemServico.diagnostico}
                  </p>
                </div>
              )}

              {/* Observações */}
              {ordemServico.observacoes && (
                <div>
                  <span className="text-sm font-medium text-gray-700">Observações:</span>
                  <p className="mt-1 whitespace-pre-wrap rounded-lg bg-gray-50 p-3 text-gray-900">
                    {ordemServico.observacoes}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Seção: Itens de Serviço */}
          <div className="rounded-lg bg-white p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900">Itens de Serviço</h2>
            <ItemOSTable items={ordemServico.itens} readOnly />
          </div>

          {/* Seção: Valores Financeiros */}
          <div className="rounded-lg bg-white p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900">Valores Financeiros</h2>
            <div className="space-y-3">
              <div className="flex justify-between border-b border-gray-200 pb-2">
                <span className="text-gray-600">Mão de Obra:</span>
                <span className="font-medium text-gray-900">
                  {formatCurrency(ordemServico.valorMaoObra)}
                </span>
              </div>
              <div className="flex justify-between border-b border-gray-200 pb-2">
                <span className="text-gray-600">Peças:</span>
                <span className="font-medium text-gray-900">
                  {formatCurrency(ordemServico.valorPecas)}
                </span>
              </div>
              <div className="flex justify-between border-b border-gray-200 pb-2">
                <span className="text-gray-600">Valor Total:</span>
                <span className="font-medium text-gray-900">
                  {formatCurrency(ordemServico.valorTotal)}
                </span>
              </div>
              {(ordemServico.descontoPercentual > 0 || ordemServico.descontoValor > 0) && (
                <div className="flex justify-between border-b border-gray-200 pb-2">
                  <span className="text-gray-600">
                    Desconto{' '}
                    {ordemServico.descontoPercentual > 0 &&
                      `(${ordemServico.descontoPercentual}%)`}
                    :
                  </span>
                  <span className="font-medium text-red-600">
                    -{' '}
                    {formatCurrency(
                      ordemServico.descontoPercentual > 0
                        ? (ordemServico.valorTotal * ordemServico.descontoPercentual) / 100
                        : ordemServico.descontoValor
                    )}
                  </span>
                </div>
              )}
              <div className="flex justify-between pt-2">
                <span className="text-lg font-semibold text-gray-900">Valor Final:</span>
                <span className="text-2xl font-bold text-green-600">
                  {formatCurrency(ordemServico.valorFinal)}
                </span>
              </div>

              {/* Aprovação do Cliente */}
              <div className="mt-4 rounded-lg bg-gray-50 p-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-700">
                    Aprovado pelo Cliente:
                  </span>
                  <span
                    className={`rounded-full px-3 py-1 text-xs font-medium ${
                      ordemServico.aprovadoPeloCliente
                        ? 'bg-green-100 text-green-800'
                        : 'bg-gray-200 text-gray-700'
                    }`}
                  >
                    {ordemServico.aprovadoPeloCliente ? 'SIM' : 'NÃO'}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Seção: Pagamentos */}
          <div className="rounded-lg bg-white p-6 shadow">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900">Pagamentos</h2>
              <button
                onClick={() => setMostrarModalPagamento(true)}
                className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
              >
                <DollarSign className="h-4 w-4" />
                Adicionar Pagamento
              </button>
            </div>

            {/* Resumo Financeiro */}
            <ResumoFinanceiro ordemServicoId={ordemServico.id} />

            {/* Lista de Pagamentos */}
            <div className="mt-6">
              <h3 className="mb-4 text-sm font-semibold text-gray-900">Histórico de Pagamentos</h3>
              <ListaPagamentos ordemServicoId={ordemServico.id} />
            </div>
          </div>
        </div>

        {/* Coluna Lateral (1/3) */}
        <div className="space-y-6">
          {/* Seção: Timeline de Status */}
          <div className="rounded-lg bg-white p-6 shadow">
            <StatusTimeline ordemServico={ordemServico} />
          </div>

          {/* Seção: Ações */}
          <div className="rounded-lg bg-white p-6 shadow">
            <h3 className="mb-4 text-sm font-medium text-gray-700">Ações Disponíveis</h3>
            <ActionButtons
              ordemServico={ordemServico}
              resumoFinanceiro={resumoFinanceiro}
              onActionComplete={() => refetch()}
            />
          </div>
        </div>
      </div>

      {/* Modal de Pagamento */}
      <PagamentoModal
        isOpen={mostrarModalPagamento}
        onClose={() => setMostrarModalPagamento(false)}
        ordemServicoId={ordemServico.id}
        valorDefault={ordemServico.valorFinal}
      />
    </div>
  );
};
