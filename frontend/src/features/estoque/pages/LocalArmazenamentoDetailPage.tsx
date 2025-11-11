import { useNavigate, useParams, Link } from 'react-router-dom';
import { MapPin, Edit, ArrowLeft, Info, Package, ExternalLink } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { useLocalArmazenamento, useLocaisFilhos } from '../hooks/useLocaisArmazenamento';
import { usePecas } from '../hooks/usePecas';
import { TipoLocalLabel, TipoLocalIcon, UnidadeMedidaSigla, getStockStatus } from '../types';
import { formatCurrency } from '@/shared/utils/formatters';

export const LocalArmazenamentoDetailPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  
  const { data: local, isLoading } = useLocalArmazenamento(id);
  const { data: filhos = [] } = useLocaisFilhos(id);

  // Buscar peças armazenadas neste local
  // Note: usePecas doesn't have a direct filter for localArmazenamentoId,
  // so we fetch all and filter client-side (not ideal for large datasets)
  const { data: todasPecas, isLoading: pecasLoading } = usePecas({});

  const pecasNoLocal = todasPecas?.content.filter(
    (peca) => peca.localArmazenamento?.id === id
  ) || [];

  if (isLoading) return <div>Carregando...</div>;
  if (!local) return <div>Local não encontrado</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold flex items-center gap-2">
          <MapPin className="h-8 w-8" />
          Detalhes do Local
        </h1>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => navigate('/estoque/locais')}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Voltar
          </Button>
          <Button onClick={() => navigate(`/estoque/locais/${id}/editar`)}>
            <Edit className="h-4 w-4 mr-2" />
            Editar
          </Button>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <div className="space-y-4 rounded-lg border p-6">
          <h2 className="text-xl font-semibold flex items-center gap-2">
            <Info className="h-5 w-5" />
            Informações Básicas
          </h2>
          <div className="space-y-3">
            <div>
              <span className="text-sm text-muted-foreground">Código</span>
              <p className="font-mono font-medium">{local.codigo}</p>
            </div>
            <div>
              <span className="text-sm text-muted-foreground">Descrição</span>
              <p className="font-medium">{local.descricao}</p>
            </div>
            <div>
              <span className="text-sm text-muted-foreground">Tipo</span>
              <p className="font-medium">
                {TipoLocalIcon[local.tipo]} {TipoLocalLabel[local.tipo]}
              </p>
            </div>
            {local.capacidadeMaxima && (
              <div>
                <span className="text-sm text-muted-foreground">Capacidade</span>
                <p className="font-medium">{local.capacidadeMaxima}</p>
              </div>
            )}
            <div>
              <span className="text-sm text-muted-foreground">Status</span>
              <p className={local.ativo ? 'text-green-600 font-medium' : 'text-gray-500'}>
                {local.ativo ? 'Ativo' : 'Inativo'}
              </p>
            </div>
          </div>
        </div>

        <div className="space-y-4 rounded-lg border p-6">
          <h2 className="text-xl font-semibold">Hierarquia</h2>
          <div className="space-y-3">
            <div>
              <span className="text-sm text-muted-foreground">Local Pai</span>
              <p className="font-medium">
                {local.localizacaoPai ? `${local.localizacaoPai.codigo} - ${local.localizacaoPai.descricao}` : 'Nenhum (Raiz)'}
              </p>
            </div>
            <div>
              <span className="text-sm text-muted-foreground">Locais Filhos ({filhos.length})</span>
              {filhos.length > 0 ? (
                <ul className="mt-2 space-y-1">
                  {filhos.map((filho) => (
                    <li key={filho.id} className="text-sm">
                      {filho.codigo} - {filho.descricao}
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-sm text-muted-foreground italic">Nenhum filho</p>
              )}
            </div>
          </div>
        </div>
      </div>

      {local.observacoes && (
        <div className="rounded-lg border p-6">
          <h2 className="text-xl font-semibold mb-3">Observações</h2>
          <p className="text-muted-foreground whitespace-pre-wrap">{local.observacoes}</p>
        </div>
      )}

      {/* Peças armazenadas neste local */}
      <div className="rounded-lg border">
        <div className="border-b bg-muted/50 p-6">
          <h2 className="text-xl font-semibold flex items-center gap-2">
            <Package className="h-5 w-5" />
            Peças Armazenadas ({pecasNoLocal.length})
          </h2>
        </div>
        <div className="p-6">
          {pecasLoading ? (
            <div className="flex justify-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            </div>
          ) : pecasNoLocal.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b text-left text-sm font-medium text-muted-foreground">
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
                      <tr key={peca.id} className="border-b hover:bg-muted/50 transition-colors">
                        <td className="py-3 font-mono text-sm">{peca.codigo}</td>
                        <td className="py-3">
                          <div>
                            <p className="font-medium">{peca.descricao}</p>
                            {peca.aplicacao && (
                              <p className="text-xs text-muted-foreground truncate max-w-xs">
                                {peca.aplicacao}
                              </p>
                            )}
                          </div>
                        </td>
                        <td className="py-3 text-sm">{peca.marca || '-'}</td>
                        <td className="py-3 text-right font-medium">
                          {peca.quantidadeAtual} {UnidadeMedidaSigla[peca.unidadeMedida]}
                        </td>
                        <td className="py-3 text-right text-sm text-muted-foreground">
                          {peca.quantidadeMinima} {UnidadeMedidaSigla[peca.unidadeMedida]}
                        </td>
                        <td className="py-3">
                          <span
                            className={`px-2 py-1 rounded-full text-xs font-medium ${status.bgColor} ${status.textColor}`}
                          >
                            {status.label}
                          </span>
                        </td>
                        <td className="py-3 text-right font-medium">
                          {formatCurrency(peca.valorVenda)}
                        </td>
                        <td className="py-3 text-right">
                          <Link
                            to={`/estoque/${peca.id}`}
                            className="inline-flex items-center gap-1 text-sm text-primary hover:underline"
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
              <Package className="h-12 w-12 mx-auto text-muted-foreground mb-3" />
              <p className="text-muted-foreground">Nenhuma peça armazenada neste local</p>
              <p className="text-sm text-muted-foreground mt-1">
                As peças aparecerão aqui quando forem vinculadas a este local
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
