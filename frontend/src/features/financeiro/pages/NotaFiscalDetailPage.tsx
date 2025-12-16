/**
 * Página de detalhes da Nota Fiscal
 */

import { Link, useParams } from 'react-router-dom';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { ArrowLeft, Edit, FileText, ExternalLink } from 'lucide-react';
import { useNotaFiscal } from '../hooks/useNotasFiscais';
import { NotaFiscalStatusBadge } from '../components/NotaFiscalStatusBadge';
import { TipoNotaFiscalLabels } from '../types/notaFiscal';

const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

export function NotaFiscalDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: notaFiscal, isLoading, error } = useNotaFiscal(id);

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <p className="text-gray-600">Carregando...</p>
      </div>
    );
  }

  if (error || !notaFiscal) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Erro ao carregar nota fiscal. Tente novamente.
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
            to="/financeiro/notas-fiscais"
            className="rounded-lg border border-gray-300 p-2 text-gray-700 hover:bg-gray-50"
          >
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div className="flex-1">
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-gray-900">
                Nota Fiscal #{notaFiscal.numero}
              </h1>
              <NotaFiscalStatusBadge status={notaFiscal.status} />
            </div>
            <p className="mt-1 text-sm text-gray-600">
              Série {notaFiscal.serie} -{' '}
              {TipoNotaFiscalLabels[notaFiscal.tipo]}
            </p>
          </div>

          {/* Botão Editar */}
          {notaFiscal.status === 'DIGITACAO' && (
            <Link
              to={`/financeiro/notas-fiscais/${notaFiscal.id}/editar`}
              className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
            >
              <Edit className="h-5 w-5" />
              Editar
            </Link>
          )}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Coluna Principal */}
        <div className="space-y-6 lg:col-span-2">
          {/* Informações Básicas */}
          <div className="rounded-lg bg-white p-6 shadow">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900">
              <FileText className="h-5 w-5" />
              Informações Básicas
            </h2>

            <div className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <span className="text-sm font-medium text-gray-700">
                    Número:
                  </span>
                  <p className="mt-1 text-gray-900">{notaFiscal.numero}</p>
                </div>
                <div>
                  <span className="text-sm font-medium text-gray-700">Série:</span>
                  <p className="mt-1 text-gray-900">{notaFiscal.serie}</p>
                </div>
                <div>
                  <span className="text-sm font-medium text-gray-700">Tipo:</span>
                  <p className="mt-1 text-gray-900">
                    {TipoNotaFiscalLabels[notaFiscal.tipo]}
                  </p>
                </div>
                <div>
                  <span className="text-sm font-medium text-gray-700">
                    Data de Emissão:
                  </span>
                  <p className="mt-1 text-gray-900">
                    {format(new Date(notaFiscal.dataEmissao), 'dd/MM/yyyy', {
                      locale: ptBR,
                    })}
                  </p>
                </div>
              </div>

              {/* Valor Total */}
              <div className="rounded-lg border border-gray-200 bg-gray-50 p-4">
                <span className="text-sm font-medium text-gray-700">
                  Valor Total:
                </span>
                <p className="mt-1 text-2xl font-bold text-green-600">
                  {formatCurrency(notaFiscal.valorTotal)}
                </p>
              </div>
            </div>
          </div>

          {/* Dados Fiscais */}
          {(notaFiscal.naturezaOperacao ||
            notaFiscal.cfop ||
            notaFiscal.informacoesComplementares) && (
            <div className="rounded-lg bg-white p-6 shadow">
              <h2 className="mb-4 text-lg font-semibold text-gray-900">
                Dados Fiscais
              </h2>

              <div className="space-y-4">
                {notaFiscal.naturezaOperacao && (
                  <div>
                    <span className="text-sm font-medium text-gray-700">
                      Natureza da Operação:
                    </span>
                    <p className="mt-1 text-gray-900">
                      {notaFiscal.naturezaOperacao}
                    </p>
                  </div>
                )}

                {notaFiscal.cfop && (
                  <div>
                    <span className="text-sm font-medium text-gray-700">
                      CFOP:
                    </span>
                    <p className="mt-1 text-gray-900">{notaFiscal.cfop}</p>
                  </div>
                )}

                {notaFiscal.informacoesComplementares && (
                  <div>
                    <span className="text-sm font-medium text-gray-700">
                      Informações Complementares:
                    </span>
                    <p className="mt-1 whitespace-pre-wrap text-gray-900">
                      {notaFiscal.informacoesComplementares}
                    </p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Dados de Autorização */}
          {(notaFiscal.chaveAcesso ||
            notaFiscal.protocoloAutorizacao ||
            notaFiscal.dataHoraAutorizacao) && (
            <div className="rounded-lg bg-white p-6 shadow">
              <h2 className="mb-4 text-lg font-semibold text-gray-900">
                Dados de Autorização
              </h2>

              <div className="space-y-4">
                {notaFiscal.chaveAcesso && (
                  <div>
                    <span className="text-sm font-medium text-gray-700">
                      Chave de Acesso:
                    </span>
                    <p className="mt-1 font-mono text-sm text-gray-900">
                      {notaFiscal.chaveAcesso}
                    </p>
                  </div>
                )}

                {notaFiscal.protocoloAutorizacao && (
                  <div>
                    <span className="text-sm font-medium text-gray-700">
                      Protocolo de Autorização:
                    </span>
                    <p className="mt-1 text-gray-900">
                      {notaFiscal.protocoloAutorizacao}
                    </p>
                  </div>
                )}

                {notaFiscal.dataHoraAutorizacao && (
                  <div>
                    <span className="text-sm font-medium text-gray-700">
                      Data/Hora da Autorização:
                    </span>
                    <p className="mt-1 text-gray-900">
                      {format(
                        new Date(notaFiscal.dataHoraAutorizacao),
                        "dd/MM/yyyy 'às' HH:mm",
                        { locale: ptBR }
                      )}
                    </p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Dados de Cancelamento */}
          {(notaFiscal.protocoloCancelamento ||
            notaFiscal.dataHoraCancelamento ||
            notaFiscal.justificativaCancelamento) && (
            <div className="rounded-lg border border-red-200 bg-red-50 p-6 shadow">
              <h2 className="mb-4 text-lg font-semibold text-red-900">
                Dados de Cancelamento
              </h2>

              <div className="space-y-4">
                {notaFiscal.protocoloCancelamento && (
                  <div>
                    <span className="text-sm font-medium text-red-700">
                      Protocolo de Cancelamento:
                    </span>
                    <p className="mt-1 text-red-900">
                      {notaFiscal.protocoloCancelamento}
                    </p>
                  </div>
                )}

                {notaFiscal.dataHoraCancelamento && (
                  <div>
                    <span className="text-sm font-medium text-red-700">
                      Data/Hora do Cancelamento:
                    </span>
                    <p className="mt-1 text-red-900">
                      {format(
                        new Date(notaFiscal.dataHoraCancelamento),
                        "dd/MM/yyyy 'às' HH:mm",
                        { locale: ptBR }
                      )}
                    </p>
                  </div>
                )}

                {notaFiscal.justificativaCancelamento && (
                  <div>
                    <span className="text-sm font-medium text-red-700">
                      Justificativa:
                    </span>
                    <p className="mt-1 text-red-900">
                      {notaFiscal.justificativaCancelamento}
                    </p>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Coluna Lateral */}
        <div className="space-y-6">
          {/* Link para OS */}
          <div className="rounded-lg bg-white p-6 shadow">
            <h3 className="mb-4 text-sm font-medium text-gray-700">
              Ordem de Serviço
            </h3>
            <Link
              to={`/ordens-servico/${notaFiscal.ordemServicoId}`}
              className="flex items-center gap-2 text-blue-600 hover:text-blue-800"
            >
              Ver Ordem de Serviço
              <ExternalLink className="h-4 w-4" />
            </Link>
          </div>

          {/* Informações do Sistema */}
          <div className="rounded-lg bg-white p-6 shadow">
            <h3 className="mb-4 text-sm font-medium text-gray-700">
              Informações do Sistema
            </h3>
            <div className="space-y-3">
              <div>
                <span className="text-xs text-gray-500">Criado em:</span>
                <p className="text-sm text-gray-900">
                  {format(
                    new Date(notaFiscal.createdAt),
                    "dd/MM/yyyy 'às' HH:mm",
                    { locale: ptBR }
                  )}
                </p>
              </div>
              <div>
                <span className="text-xs text-gray-500">
                  Última atualização:
                </span>
                <p className="text-sm text-gray-900">
                  {format(
                    new Date(notaFiscal.updatedAt),
                    "dd/MM/yyyy 'às' HH:mm",
                    { locale: ptBR }
                  )}
                </p>
              </div>
            </div>
          </div>

          {/* Alert sobre SEFAZ */}
          <div className="rounded-lg border border-yellow-200 bg-yellow-50 p-4">
            <p className="text-xs text-yellow-800">
              <strong>⚠️ Nota:</strong> A emissão e autorização via SEFAZ serão
              implementadas na Fase 3 do projeto.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
