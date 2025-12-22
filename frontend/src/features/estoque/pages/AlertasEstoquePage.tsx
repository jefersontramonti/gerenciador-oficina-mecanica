/**
 * Página de Alertas de Estoque (Baixo e Zerado)
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, AlertTriangle, XCircle, ArrowDownCircle } from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/components/ui/tabs';
import { Button } from '@/shared/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/shared/components/ui/table';
import { Badge } from '@/shared/components/ui/badge';
import { formatCurrency } from '@/shared/utils/formatters';
import { useAlertasEstoqueBaixo, useAlertasEstoqueZerado } from '../hooks/usePecas';
import { StockBadge, UnidadeMedidaBadge, MovimentacaoModal } from '../components';
import type { Peca } from '../types';

export const AlertasEstoquePage = () => {
  const navigate = useNavigate();

  const [pageBaixo, setPageBaixo] = useState(0);
  const [pageZerado, setPageZerado] = useState(0);

  const { data: dataBaixo, isLoading: isLoadingBaixo } = useAlertasEstoqueBaixo(
    pageBaixo,
    20
  );
  const { data: dataZerado, isLoading: isLoadingZerado } = useAlertasEstoqueZerado(
    pageZerado,
    20
  );

  const [movimentacaoModal, setMovimentacaoModal] = useState<{
    isOpen: boolean;
    peca: Peca | null;
  }>({
    isOpen: false,
    peca: null,
  });

  const handleRegistrarEntrada = (peca: Peca) => {
    setMovimentacaoModal({ isOpen: true, peca });
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <Button variant="ghost" size="sm" onClick={() => navigate('/estoque')}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          Voltar para Estoque
        </Button>
        <h1 className="text-3xl font-bold tracking-tight mt-2 flex items-center gap-2">
          <AlertTriangle className="h-8 w-8 text-orange-600 dark:text-orange-400" />
          Alertas de Estoque
        </h1>
        <p className="text-muted-foreground">
          Peças que requerem atenção imediata
        </p>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="baixo" className="space-y-4">
        <TabsList className="grid w-full max-w-md grid-cols-2">
          <TabsTrigger value="baixo" className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4" />
            Estoque Baixo
            {dataBaixo && (
              <Badge variant="secondary">{dataBaixo.totalElements}</Badge>
            )}
          </TabsTrigger>
          <TabsTrigger value="zerado" className="flex items-center gap-2">
            <XCircle className="h-4 w-4" />
            Estoque Zerado
            {dataZerado && (
              <Badge variant="destructive">{dataZerado.totalElements}</Badge>
            )}
          </TabsTrigger>
        </TabsList>

        {/* Tab: Estoque Baixo */}
        <TabsContent value="baixo" className="space-y-4">
          <div className="bg-orange-50 dark:bg-orange-950/30 border border-orange-200 dark:border-orange-800 rounded-lg p-4">
            <p className="text-sm text-orange-800 dark:text-orange-300">
              <strong>Atenção:</strong> As peças abaixo estão com quantidade atual{' '}
              <strong>igual ou inferior</strong> à quantidade mínima configurada.
            </p>
          </div>

          <div className="bg-card rounded-lg border">
            {isLoadingBaixo ? (
              <div className="flex justify-center items-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
              </div>
            ) : !dataBaixo || dataBaixo.content.length === 0 ? (
              <div className="text-center py-12 text-muted-foreground">
                Nenhuma peça com estoque baixo
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Código</TableHead>
                    <TableHead>Descrição</TableHead>
                    <TableHead className="text-center">Unidade</TableHead>
                    <TableHead className="text-right">Qtd Atual</TableHead>
                    <TableHead className="text-right">Qtd Mínima</TableHead>
                    <TableHead className="text-center">Status</TableHead>
                    <TableHead className="text-right">Valor Unit.</TableHead>
                    <TableHead className="text-right">Ações</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {dataBaixo.content.map((peca) => (
                    <TableRow key={peca.id}>
                      <TableCell className="font-medium">{peca.codigo}</TableCell>
                      <TableCell className="max-w-xs truncate">
                        {peca.descricao}
                      </TableCell>
                      <TableCell className="text-center">
                        <UnidadeMedidaBadge unidade={peca.unidadeMedida} />
                      </TableCell>
                      <TableCell className="text-right font-medium text-orange-600 dark:text-orange-400">
                        {peca.quantidadeAtual}
                      </TableCell>
                      <TableCell className="text-right">
                        {peca.quantidadeMinima}
                      </TableCell>
                      <TableCell className="text-center">
                        <StockBadge
                          quantidadeAtual={peca.quantidadeAtual}
                          quantidadeMinima={peca.quantidadeMinima}
                        />
                      </TableCell>
                      <TableCell className="text-right">
                        {formatCurrency(peca.valorCusto)}
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          size="sm"
                          variant="outline"
                          className="text-green-600 dark:text-green-400 hover:text-green-700 dark:hover:text-green-300"
                          onClick={() => handleRegistrarEntrada(peca)}
                        >
                          <ArrowDownCircle className="h-4 w-4 mr-1" />
                          Entrada
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>

          {/* Paginação */}
          {dataBaixo && dataBaixo.totalPages > 1 && (
            <div className="flex justify-between items-center">
              <p className="text-sm text-muted-foreground">
                Mostrando {dataBaixo.content.length} de {dataBaixo.totalElements}{' '}
                peças
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={dataBaixo.first}
                  onClick={() => setPageBaixo((prev) => prev - 1)}
                >
                  Anterior
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={dataBaixo.last}
                  onClick={() => setPageBaixo((prev) => prev + 1)}
                >
                  Próxima
                </Button>
              </div>
            </div>
          )}
        </TabsContent>

        {/* Tab: Estoque Zerado */}
        <TabsContent value="zerado" className="space-y-4">
          <div className="bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-800 rounded-lg p-4">
            <p className="text-sm text-red-800 dark:text-red-300">
              <strong>Urgente:</strong> As peças abaixo estão com{' '}
              <strong>quantidade zerada</strong>. Registre entradas imediatamente para
              evitar problemas operacionais.
            </p>
          </div>

          <div className="bg-card rounded-lg border">
            {isLoadingZerado ? (
              <div className="flex justify-center items-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
              </div>
            ) : !dataZerado || dataZerado.content.length === 0 ? (
              <div className="text-center py-12 text-muted-foreground">
                Nenhuma peça com estoque zerado
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Código</TableHead>
                    <TableHead>Descrição</TableHead>
                    <TableHead className="text-center">Unidade</TableHead>
                    <TableHead className="text-right">Qtd Mínima</TableHead>
                    <TableHead className="text-center">Status</TableHead>
                    <TableHead className="text-right">Valor Unit.</TableHead>
                    <TableHead className="text-right">Ações</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {dataZerado.content.map((peca) => (
                    <TableRow key={peca.id} className="bg-red-50/50 dark:bg-red-950/20">
                      <TableCell className="font-medium">{peca.codigo}</TableCell>
                      <TableCell className="max-w-xs truncate">
                        {peca.descricao}
                      </TableCell>
                      <TableCell className="text-center">
                        <UnidadeMedidaBadge unidade={peca.unidadeMedida} />
                      </TableCell>
                      <TableCell className="text-right">
                        {peca.quantidadeMinima}
                      </TableCell>
                      <TableCell className="text-center">
                        <StockBadge
                          quantidadeAtual={peca.quantidadeAtual}
                          quantidadeMinima={peca.quantidadeMinima}
                        />
                      </TableCell>
                      <TableCell className="text-right">
                        {formatCurrency(peca.valorCusto)}
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          size="sm"
                          className="bg-green-600 hover:bg-green-700 dark:bg-green-700 dark:hover:bg-green-600"
                          onClick={() => handleRegistrarEntrada(peca)}
                        >
                          <ArrowDownCircle className="h-4 w-4 mr-1" />
                          Entrada Urgente
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>

          {/* Paginação */}
          {dataZerado && dataZerado.totalPages > 1 && (
            <div className="flex justify-between items-center">
              <p className="text-sm text-muted-foreground">
                Mostrando {dataZerado.content.length} de {dataZerado.totalElements}{' '}
                peças
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={dataZerado.first}
                  onClick={() => setPageZerado((prev) => prev - 1)}
                >
                  Anterior
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={dataZerado.last}
                  onClick={() => setPageZerado((prev) => prev + 1)}
                >
                  Próxima
                </Button>
              </div>
            </div>
          )}
        </TabsContent>
      </Tabs>

      {/* Modal de Entrada */}
      {movimentacaoModal.peca && (
        <MovimentacaoModal
          isOpen={movimentacaoModal.isOpen}
          onClose={() => setMovimentacaoModal({ isOpen: false, peca: null })}
          tipo="ENTRADA"
          peca={{
            id: movimentacaoModal.peca.id,
            codigo: movimentacaoModal.peca.codigo,
            descricao: movimentacaoModal.peca.descricao,
            unidadeMedida: movimentacaoModal.peca.unidadeMedida,
            quantidadeAtual: movimentacaoModal.peca.quantidadeAtual,
          }}
        />
      )}
    </div>
  );
};
