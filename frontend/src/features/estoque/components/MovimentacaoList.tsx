/**
 * Lista/Tabela de Movimentações de Estoque
 */

import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/shared/components/ui/table';
import { formatCurrency } from '@/shared/utils/formatters';
import type { MovimentacaoEstoque } from '../types';
import {
  TipoMovimentacao,
  TipoMovimentacaoLabel,
  TipoMovimentacaoColor,
  getMovimentacaoSinal,
} from '../types';

interface MovimentacaoListProps {
  movimentacoes: MovimentacaoEstoque[];
  isLoading?: boolean;
  showPecaInfo?: boolean;
}

const TipoMovimentacaoBadge = ({ tipo }: { tipo: TipoMovimentacao }) => {
  const colorMap: Record<string, string> = {
    green: 'bg-green-100 text-green-800',
    red: 'bg-red-100 text-red-800',
    yellow: 'bg-yellow-100 text-yellow-800',
    blue: 'bg-blue-100 text-blue-800',
    gray: 'bg-gray-100 text-gray-800',
  };

  const color = TipoMovimentacaoColor[tipo];
  const colorClass = colorMap[color] || colorMap.gray;

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${colorClass}`}>
      {TipoMovimentacaoLabel[tipo]}
    </span>
  );
};

export const MovimentacaoList = ({
  movimentacoes,
  isLoading = false,
  showPecaInfo = false,
}: MovimentacaoListProps) => {
  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  if (!movimentacoes || movimentacoes.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        Nenhuma movimentação encontrada
      </div>
    );
  }

  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Data/Hora</TableHead>
            {showPecaInfo && <TableHead>Peça</TableHead>}
            <TableHead>Tipo</TableHead>
            <TableHead className="text-right">Quantidade</TableHead>
            <TableHead className="text-right">Qtd Anterior</TableHead>
            <TableHead className="text-right">Qtd Atual</TableHead>
            <TableHead className="text-right">Valor Unit.</TableHead>
            <TableHead className="text-right">Valor Total</TableHead>
            <TableHead>Usuário</TableHead>
            <TableHead>Motivo</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {movimentacoes.map((mov) => {
            const sinal = getMovimentacaoSinal(mov.tipo);
            const quantidadeFormatada = `${sinal}${mov.quantidade}`;

            // Formata data com validação
            const formatarData = () => {
              if (!mov.dataMovimentacao) return '-';

              try {
                const date = new Date(mov.dataMovimentacao);
                if (isNaN(date.getTime())) return '-';

                return format(date, 'dd/MM/yyyy HH:mm', { locale: ptBR });
              } catch (error) {
                console.error('Erro ao formatar data:', error, mov.dataMovimentacao);
                return '-';
              }
            };

            return (
              <TableRow key={mov.id}>
                <TableCell className="whitespace-nowrap">
                  {formatarData()}
                </TableCell>

                {showPecaInfo && (
                  <TableCell>
                    <div className="flex flex-col">
                      <span className="font-medium">{mov.peca.codigo}</span>
                      <span className="text-sm text-muted-foreground">
                        {mov.peca.descricao}
                      </span>
                    </div>
                  </TableCell>
                )}

                <TableCell>
                  <TipoMovimentacaoBadge tipo={mov.tipo} />
                </TableCell>

                <TableCell className="text-right font-medium">
                  <span
                    className={
                      sinal === '+'
                        ? 'text-green-600'
                        : sinal === '-'
                        ? 'text-red-600'
                        : 'text-yellow-600'
                    }
                  >
                    {quantidadeFormatada}
                  </span>
                </TableCell>

                <TableCell className="text-right text-muted-foreground">
                  {mov.quantidadeAnterior}
                </TableCell>

                <TableCell className="text-right font-medium">
                  {mov.quantidadeAtual}
                </TableCell>

                <TableCell className="text-right">
                  {formatCurrency(mov.valorUnitario)}
                </TableCell>

                <TableCell className="text-right font-medium">
                  {formatCurrency(mov.valorTotal)}
                </TableCell>

                <TableCell>
                  <div className="flex flex-col">
                    <span className="text-sm">{mov.usuario.nome}</span>
                    {mov.numeroOS && (
                      <span className="text-xs text-muted-foreground">
                        OS #{mov.numeroOS}
                      </span>
                    )}
                  </div>
                </TableCell>

                <TableCell className="max-w-xs">
                  <div className="flex flex-col gap-1">
                    {mov.motivo && (
                      <span className="text-sm">{mov.motivo}</span>
                    )}
                    {mov.observacao && (
                      <span className="text-xs text-muted-foreground">
                        {mov.observacao}
                      </span>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
};
