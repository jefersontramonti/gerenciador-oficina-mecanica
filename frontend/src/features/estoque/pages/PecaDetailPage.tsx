/**
 * Página de detalhes da Peça com histórico de movimentações
 */

import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  Edit,
  Power,
  PowerOff,
  ArrowDownCircle,
  ArrowUpCircle,
  Settings,
  Package,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { Badge } from '@/shared/components/ui/badge';
import { Separator } from '@/shared/components/ui/separator';
import { formatCurrency } from '@/shared/utils/formatters';
import { usePeca, useDesativarPeca, useReativarPeca } from '../hooks/usePecas';
import { useHistoricoPeca } from '../hooks/useMovimentacoes';
import {
  StockBadge,
  UnidadeMedidaBadge,
  MovimentacaoList,
  MovimentacaoModal,
} from '../components';
import { UnidadeMedidaLabel, getMargemLucroStatus } from '../types';

export const PecaDetailPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();

  console.log('PecaDetailPage - ID from params:', id);

  const { data: peca, isLoading, error, refetch } = usePeca(id);
  const { data: movimentacoesData, isLoading: isLoadingMovimentacoes } =
    useHistoricoPeca(id);
  const desativarPeca = useDesativarPeca();
  const reativarPeca = useReativarPeca();

  console.log('PecaDetailPage - peca:', peca);
  console.log('PecaDetailPage - isLoading:', isLoading);
  console.log('PecaDetailPage - error:', error);

  const [movimentacaoModal, setMovimentacaoModal] = useState<{
    isOpen: boolean;
    tipo: 'ENTRADA' | 'SAIDA' | 'AJUSTE';
  }>({
    isOpen: false,
    tipo: 'ENTRADA',
  });

  const handleDesativar = async () => {
    if (id && confirm('Deseja realmente desativar esta peça?')) {
      await desativarPeca.mutateAsync(id);
    }
  };

  const handleReativar = async () => {
    if (id) {
      await reativarPeca.mutateAsync(id);
    }
  };

  const handleMovimentar = (tipo: 'ENTRADA' | 'SAIDA' | 'AJUSTE') => {
    setMovimentacaoModal({ isOpen: true, tipo });
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        <p className="ml-3 text-muted-foreground">Carregando dados da peça...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <div className="bg-destructive/10 border border-destructive rounded-lg p-6 max-w-md mx-auto">
          <h2 className="text-lg font-semibold text-destructive mb-2">Erro ao carregar peça</h2>
          <p className="text-muted-foreground mb-4">
            {error instanceof Error ? error.message : 'Erro desconhecido ao buscar dados da peça'}
          </p>
          <div className="flex gap-2 justify-center">
            <Button variant="outline" onClick={() => refetch()}>
              Tentar Novamente
            </Button>
            <Button variant="default" onClick={() => navigate('/estoque')}>
              Voltar para listagem
            </Button>
          </div>
        </div>
      </div>
    );
  }

  if (!peca) {
    return (
      <div className="text-center py-12">
        <div className="bg-muted border rounded-lg p-6 max-w-md mx-auto">
          <h2 className="text-lg font-semibold mb-2">Peça não encontrada</h2>
          <p className="text-muted-foreground mb-4">
            A peça com ID "{id}" não foi encontrada no sistema.
          </p>
          <Button variant="default" onClick={() => navigate('/estoque')}>
            Voltar para listagem
          </Button>
        </div>
      </div>
    );
  }

  const margemStatus = getMargemLucroStatus(peca.margemLucro);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <Button variant="ghost" size="sm" onClick={() => navigate('/estoque')}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          Voltar
        </Button>
        <div className="flex items-start justify-between mt-2">
          <div>
            <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
              <Package className="h-8 w-8" />
              {peca.codigo}
            </h1>
            <p className="text-muted-foreground mt-1">{peca.descricao}</p>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => navigate(`/estoque/${id}/editar`)}>
              <Edit className="h-4 w-4 mr-2" />
              Editar
            </Button>
            {peca.ativo ? (
              <Button variant="destructive" onClick={handleDesativar}>
                <PowerOff className="h-4 w-4 mr-2" />
                Desativar
              </Button>
            ) : (
              <Button variant="default" onClick={handleReativar}>
                <Power className="h-4 w-4 mr-2" />
                Reativar
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* Informações Principais */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-card p-4 rounded-lg border">
          <p className="text-sm text-muted-foreground">Status</p>
          <div className="mt-2 flex items-center gap-2">
            <StockBadge
              quantidadeAtual={peca.quantidadeAtual}
              quantidadeMinima={peca.quantidadeMinima}
            />
            {!peca.ativo && (
              <Badge variant="secondary">Inativo</Badge>
            )}
          </div>
        </div>

        <div className="bg-card p-4 rounded-lg border">
          <p className="text-sm text-muted-foreground">Quantidade Atual</p>
          <p className="text-2xl font-bold mt-1">{peca.quantidadeAtual}</p>
          <p className="text-xs text-muted-foreground mt-1">
            Mínimo: {peca.quantidadeMinima}
          </p>
        </div>

        <div className="bg-card p-4 rounded-lg border">
          <p className="text-sm text-muted-foreground">Valor em Estoque</p>
          <p className="text-2xl font-bold mt-1">
            {formatCurrency(peca.valorTotalEstoque)}
          </p>
          <p className="text-xs text-muted-foreground mt-1">
            Custo unitário: {formatCurrency(peca.valorCusto)}
          </p>
        </div>

        <div className="bg-card p-4 rounded-lg border">
          <p className="text-sm text-muted-foreground">Margem de Lucro</p>
          <p className="text-2xl font-bold mt-1">{peca.margemLucro.toFixed(2)}%</p>
          <span
            className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium mt-1 ${margemStatus.bgColor} ${margemStatus.textColor}`}
          >
            {margemStatus.label}
          </span>
        </div>
      </div>

      {/* Detalhes da Peça */}
      <div className="bg-card p-6 rounded-lg border space-y-4">
        <h2 className="text-lg font-semibold">Informações Detalhadas</h2>
        <Separator />

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Código (SKU)</p>
            <p className="font-medium">{peca.codigo}</p>
          </div>

          <div>
            <p className="text-sm text-muted-foreground">Unidade de Medida</p>
            <div className="mt-1">
              <UnidadeMedidaBadge unidade={peca.unidadeMedida} />
              <span className="ml-2 text-sm">
                {UnidadeMedidaLabel[peca.unidadeMedida]}
              </span>
            </div>
          </div>

          {peca.marca && (
            <div>
              <p className="text-sm text-muted-foreground">Marca</p>
              <p className="font-medium">{peca.marca}</p>
            </div>
          )}

          {peca.localizacao && (
            <div>
              <p className="text-sm text-muted-foreground">Localização</p>
              <p className="font-medium">{peca.localizacao}</p>
            </div>
          )}

          <div>
            <p className="text-sm text-muted-foreground">Valor de Custo</p>
            <p className="font-medium">{formatCurrency(peca.valorCusto)}</p>
          </div>

          <div>
            <p className="text-sm text-muted-foreground">Valor de Venda</p>
            <p className="font-medium">{formatCurrency(peca.valorVenda)}</p>
          </div>
        </div>

        {peca.aplicacao && (
          <div>
            <p className="text-sm text-muted-foreground">Aplicação</p>
            <p className="mt-1">{peca.aplicacao}</p>
          </div>
        )}
      </div>

      {/* Ações de Movimentação */}
      <div className="bg-card p-6 rounded-lg border">
        <h2 className="text-lg font-semibold mb-4">Movimentar Estoque</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Button
            variant="outline"
            size="lg"
            className="h-auto flex-col gap-2 py-6 hover:bg-green-50 hover:border-green-300"
            onClick={() => handleMovimentar('ENTRADA')}
          >
            <ArrowDownCircle className="h-8 w-8 text-green-600" />
            <span className="font-medium">Registrar Entrada</span>
            <span className="text-xs text-muted-foreground">
              Adicionar itens ao estoque
            </span>
          </Button>

          <Button
            variant="outline"
            size="lg"
            className="h-auto flex-col gap-2 py-6 hover:bg-red-50 hover:border-red-300"
            onClick={() => handleMovimentar('SAIDA')}
          >
            <ArrowUpCircle className="h-8 w-8 text-red-600" />
            <span className="font-medium">Registrar Saída</span>
            <span className="text-xs text-muted-foreground">
              Remover itens do estoque
            </span>
          </Button>

          <Button
            variant="outline"
            size="lg"
            className="h-auto flex-col gap-2 py-6 hover:bg-yellow-50 hover:border-yellow-300"
            onClick={() => handleMovimentar('AJUSTE')}
          >
            <Settings className="h-8 w-8 text-yellow-600" />
            <span className="font-medium">Ajustar Inventário</span>
            <span className="text-xs text-muted-foreground">
              Corrigir quantidade em estoque
            </span>
          </Button>
        </div>
      </div>

      {/* Histórico de Movimentações */}
      <div className="bg-card p-6 rounded-lg border">
        <h2 className="text-lg font-semibold mb-4">Histórico de Movimentações</h2>
        <MovimentacaoList
          movimentacoes={movimentacoesData?.content || []}
          isLoading={isLoadingMovimentacoes}
          showPecaInfo={false}
        />
      </div>

      {/* Modal de Movimentação */}
      <MovimentacaoModal
        isOpen={movimentacaoModal.isOpen}
        onClose={() =>
          setMovimentacaoModal({ isOpen: false, tipo: 'ENTRADA' })
        }
        onSuccess={() => refetch()}
        tipo={movimentacaoModal.tipo}
        peca={{
          id: peca.id,
          codigo: peca.codigo,
          descricao: peca.descricao,
          unidadeMedida: peca.unidadeMedida,
          quantidadeAtual: peca.quantidadeAtual,
        }}
      />
    </div>
  );
};
