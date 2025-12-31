import { useNavigate, useParams, Link } from 'react-router-dom';
import { ArrowLeft, Edit, ExternalLink } from 'lucide-react';
import { useLocalArmazenamento, useLocaisFilhos } from '../hooks/useLocaisArmazenamento';
import { usePecas } from '../hooks/usePecas';
import { TipoLocalLabel, TipoLocalIcon, UnidadeMedidaSigla, getStockStatus } from '../types';
import { formatCurrency } from '@/shared/utils/formatters';

export const LocalArmazenamentoDetailPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();

  const { data: local, isLoading } = useLocalArmazenamento(id);
  const { data: filhos = [] } = useLocaisFilhos(id);
  const { data: pecasData, isLoading: pecasLoading } = usePecas(
    id ? { localArmazenamentoId: id, size: 100 } : {}
  );

  const pecasNoLocal = pecasData?.content || [];

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Carregando...</div>
      </div>
    );
  }

  if (!local) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400 dark:text-red-300">
          Local não encontrado
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/estoque/locais')}
            className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <ArrowLeft className="h-5 w-5 text-gray-900 dark:text-gray-100" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{local.codigo}</h1>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              {local.descricao}
            </p>
          </div>
        </div>

        <div className="flex gap-2">
          <button
            onClick={() => navigate(`/estoque/locais/${id}/editar`)}
            className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
          >
            <Edit className="h-5 w-5" />
            Editar
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Main Info */}
        <div className="lg:col-span-2 space-y-6">
          {/* Informações Básicas */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Informações Básicas</h2>

            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Código</label>
                  <p className="mt-1 font-mono text-gray-900 dark:text-gray-100">{local.codigo}</p>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Tipo</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100">
                    {TipoLocalIcon[local.tipo]} {TipoLocalLabel[local.tipo]}
                  </p>
                </div>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Descrição</label>
                <p className="mt-1 text-gray-900 dark:text-gray-100">{local.descricao}</p>
              </div>

              {local.capacidadeMaxima && (
                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Capacidade Máxima</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100">{local.capacidadeMaxima}</p>
                </div>
              )}
            </div>
          </div>

          {/* Hierarquia */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Hierarquia</h2>

            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Local Pai</label>
                <p className="mt-1 text-gray-900 dark:text-gray-100">
                  {local.localizacaoPai ? `${local.localizacaoPai.codigo} - ${local.localizacaoPai.descricao}` : 'Nenhum (Raiz)'}
                </p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Locais Filhos ({filhos.length})
                </label>
                {filhos.length > 0 ? (
                  <ul className="mt-2 space-y-1">
                    {filhos.map((filho) => (
                      <li key={filho.id} className="text-sm text-gray-900 dark:text-gray-100">
                        {filho.codigo} - {filho.descricao}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="mt-1 text-sm italic text-gray-500 dark:text-gray-400">Nenhum filho</p>
                )}
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Caminho Completo</label>
                <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">{local.caminhoCompleto}</p>
              </div>
            </div>
          </div>

          {local.observacoes && (
            <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
              <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Observações</h2>
              <p className="whitespace-pre-wrap text-gray-900 dark:text-gray-100">{local.observacoes}</p>
            </div>
          )}

          {/* Peças armazenadas */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
              Peças Armazenadas ({pecasNoLocal.length})
            </h2>

            {pecasLoading ? (
              <div className="flex h-32 items-center justify-center">
                <div className="text-gray-500 dark:text-gray-400">Carregando peças...</div>
              </div>
            ) : pecasNoLocal.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-gray-200 dark:border-gray-700 text-left text-sm font-medium text-gray-500 dark:text-gray-400">
                      <th className="pb-3">Código</th>
                      <th className="pb-3">Descrição</th>
                      <th className="pb-3">Marca</th>
                      <th className="pb-3 text-right">Qtd. Atual</th>
                      <th className="pb-3 text-right">Qtd. Mínima</th>
                      <th className="pb-3">Status</th>
                      <th className="pb-3 text-right">Valor</th>
                      <th className="pb-3"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {pecasNoLocal.map((peca) => {
                      const status = getStockStatus(peca.quantidadeAtual, peca.quantidadeMinima);
                      return (
                        <tr key={peca.id} className="border-b border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                          <td className="py-3 font-mono text-sm text-gray-900 dark:text-gray-100">{peca.codigo}</td>
                          <td className="py-3">
                            <div>
                              <p className="font-medium text-gray-900 dark:text-gray-100">{peca.descricao}</p>
                              {peca.aplicacao && (
                                <p className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-xs">
                                  {peca.aplicacao}
                                </p>
                              )}
                            </div>
                          </td>
                          <td className="py-3 text-sm text-gray-900 dark:text-gray-100">{peca.marca || '-'}</td>
                          <td className="py-3 text-right font-medium text-gray-900 dark:text-gray-100">
                            {peca.quantidadeAtual} {UnidadeMedidaSigla[peca.unidadeMedida]}
                          </td>
                          <td className="py-3 text-right text-sm text-gray-500 dark:text-gray-400">
                            {peca.quantidadeMinima} {UnidadeMedidaSigla[peca.unidadeMedida]}
                          </td>
                          <td className="py-3">
                            <span
                              className={`px-2 py-1 rounded-full text-xs font-medium ${status.bgColor} ${status.textColor}`}
                            >
                              {status.label}
                            </span>
                          </td>
                          <td className="py-3 text-right font-medium text-gray-900 dark:text-gray-100">
                            {formatCurrency(peca.valorVenda)}
                          </td>
                          <td className="py-3 text-right">
                            <Link
                              to={`/estoque/${peca.id}`}
                              className="inline-flex items-center gap-1 text-sm text-blue-600 dark:text-blue-400 hover:underline"
                            >
                              Ver
                              <ExternalLink className="h-3 w-3" />
                            </Link>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="text-center py-12">
                <p className="text-gray-500 dark:text-gray-400">Nenhuma peça armazenada neste local</p>
                <p className="text-sm text-gray-400 dark:text-gray-500 mt-1">
                  As peças aparecerão aqui quando forem vinculadas a este local
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Status */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h3 className="mb-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Status</h3>
            {local.ativo ? (
              <span className="inline-flex rounded-full bg-green-100 dark:bg-green-900/30 px-3 py-1 text-sm font-semibold text-green-800 dark:text-green-400">
                Ativo
              </span>
            ) : (
              <span className="inline-flex rounded-full bg-gray-100 dark:bg-gray-700 px-3 py-1 text-sm font-semibold text-gray-800 dark:text-gray-300">
                Inativo
              </span>
            )}
          </div>

          {/* Metadata */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h3 className="mb-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Informações</h3>

            <div className="space-y-3">
              <div>
                <label className="text-xs font-medium text-gray-500 dark:text-gray-400">Nível na Hierarquia</label>
                <p className="mt-1 text-sm text-gray-900 dark:text-gray-100">{local.nivel}</p>
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500 dark:text-gray-400">É Raiz?</label>
                <p className="mt-1 text-sm text-gray-900 dark:text-gray-100">{local.isRaiz ? 'Sim' : 'Não'}</p>
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500 dark:text-gray-400">Tem Filhos?</label>
                <p className="mt-1 text-sm text-gray-900 dark:text-gray-100">{local.temFilhos ? 'Sim' : 'Não'}</p>
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500 dark:text-gray-400">ID</label>
                <p className="mt-1 text-xs font-mono text-gray-600 dark:text-gray-400">{local.id}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
