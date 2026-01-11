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
    green: 'bg-green-100 dark:bg-green-950/40 text-green-800 dark:text-green-300',
    red: 'bg-red-100 dark:bg-red-950/40 text-red-800 dark:text-red-300',
    yellow: 'bg-yellow-100 dark:bg-yellow-950/40 text-yellow-800 dark:text-yellow-300',
    blue: 'bg-blue-100 dark:bg-blue-950/40 text-blue-800 dark:text-blue-300',
    gray: 'bg-gray-100 dark:bg-gray-800 text-gray-800 dark:text-gray-300',
  };

  const color = TipoMovimentacaoColor[tipo];
  const colorClass = colorMap[color] || colorMap.gray;

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${colorClass}`}>
      {TipoMovimentacaoLabel[tipo]}
    </span>
  );
};

// Função para formatar data com validação
const formatarData = (dataMovimentacao: string | undefined) => {
  if (!dataMovimentacao) return '-';

  try {
    const date = new Date(dataMovimentacao);
    if (isNaN(date.getTime())) return '-';

    return format(date, 'dd/MM/yyyy HH:mm', { locale: ptBR });
  } catch (error) {
    console.error('Erro ao formatar data:', error, dataMovimentacao);
    return '-';
  }
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
    <>
      {/* Mobile: Card Layout */}
      <div className="space-y-3 lg:hidden">
        {movimentacoes.map((mov) => {
          const sinal = getMovimentacaoSinal(mov.tipo);
          const quantidadeFormatada = `${sinal}${mov.quantidade}`;

          return (
            <div
              key={mov.id}
              className="rounded-lg border border-gray-200 dark:border-gray-700 p-3"
            >
              {/* Header: Data, Tipo e Quantidade */}
              <div className="flex items-start justify-between gap-2 mb-2">
                <div className="flex flex-col gap-1">
                  <span className="text-xs text-gray-500 dark:text-gray-400">
                    {formatarData(mov.dataMovimentacao)}
                  </span>
                  <TipoMovimentacaoBadge tipo={mov.tipo} />
                </div>
                <div className="text-right">
                  <span
                    className={`text-lg font-bold ${
                      sinal === '+'
                        ? 'text-green-600 dark:text-green-400'
                        : sinal === '-'
                        ? 'text-red-600 dark:text-red-400'
                        : 'text-yellow-600 dark:text-yellow-400'
                    }`}
                  >
                    {quantidadeFormatada}
                  </span>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    {mov.quantidadeAnterior} → {mov.quantidadeAtual}
                  </p>
                </div>
              </div>

              {/* Peça info (se showPecaInfo) */}
              {showPecaInfo && (
                <div className="mb-2 pb-2 border-b border-gray-200 dark:border-gray-700">
                  <span className="font-mono text-sm font-medium text-gray-900 dark:text-gray-100">
                    {mov.peca.codigo}
                  </span>
                  <p className="text-sm text-gray-600 dark:text-gray-400 truncate">
                    {mov.peca.descricao}
                  </p>
                </div>
              )}

              {/* Valores e Usuário */}
              <div className="grid grid-cols-2 gap-2 text-sm">
                <div>
                  <span className="text-gray-500 dark:text-gray-400">Valor unit:</span>
                  <span className="ml-1 font-medium text-gray-900 dark:text-gray-100">
                    {formatCurrency(mov.valorUnitario)}
                  </span>
                </div>
                <div>
                  <span className="text-gray-500 dark:text-gray-400">Total:</span>
                  <span className="ml-1 font-medium text-gray-900 dark:text-gray-100">
                    {formatCurrency(mov.valorTotal)}
                  </span>
                </div>
              </div>

              {/* Usuário */}
              <div className="mt-2 text-sm">
                <span className="text-gray-500 dark:text-gray-400">Por:</span>
                <span className="ml-1 text-gray-900 dark:text-gray-100">
                  {mov.usuario.nome}
                </span>
                {mov.numeroOS && (
                  <span className="ml-2 text-xs text-blue-600 dark:text-blue-400">
                    OS #{mov.numeroOS}
                  </span>
                )}
              </div>

              {/* Motivo/Observação */}
              {(mov.motivo || mov.observacao) && (
                <div className="mt-2 pt-2 border-t border-gray-200 dark:border-gray-700">
                  {mov.motivo && (
                    <p className="text-sm text-gray-900 dark:text-gray-100">{mov.motivo}</p>
                  )}
                  {mov.observacao && (
                    <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      {mov.observacao}
                    </p>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Desktop: Table Layout */}
      <div className="hidden lg:block rounded-md border">
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

              return (
                <TableRow key={mov.id}>
                  <TableCell className="whitespace-nowrap">
                    {formatarData(mov.dataMovimentacao)}
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
                          ? 'text-green-600 dark:text-green-400'
                          : sinal === '-'
                          ? 'text-red-600 dark:text-red-400'
                          : 'text-yellow-600 dark:text-yellow-400'
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
    </>
  );
};
