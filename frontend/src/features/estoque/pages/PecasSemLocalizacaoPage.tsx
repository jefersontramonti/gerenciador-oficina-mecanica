/**
 * Página para listar e gerenciar peças sem localização definida
 */

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { MapPinOff, Package, ExternalLink, MapPin } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { usePecasSemLocalizacao, useDefinirLocalizacao } from '../hooks/usePecas';
import { UnidadeMedidaSigla, getStockStatus } from '../types';
import { formatCurrency } from '@/shared/utils/formatters';
import { LocalArmazenamentoSelect } from '../components';

export const PecasSemLocalizacaoPage = () => {
  const [page] = useState(0);
  const size = 50; // Mostrar mais itens por página

  const { data, isLoading } = usePecasSemLocalizacao(page, size);
  const definirLocalizacao = useDefinirLocalizacao();

  const [selectedPecaId, setSelectedPecaId] = useState<string | null>(null);
  const [selectedLocalId, setSelectedLocalId] = useState<string>('');

  const pecas = data?.content || [];

  const handleAssignLocation = async (pecaId: string) => {
    if (!selectedLocalId) {
      alert('Selecione um local de armazenamento');
      return;
    }

    try {
      await definirLocalizacao.mutateAsync({
        pecaId,
        localId: selectedLocalId,
      });
      // Reset selection
      setSelectedPecaId(null);
      setSelectedLocalId('');
    } catch (error) {
      // Error handled by mutation
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <div className="flex items-center gap-3 mb-2">
          <div className="rounded-full bg-orange-100 dark:bg-orange-950/40 p-2">
            <MapPinOff className="h-6 w-6 text-orange-600 dark:text-orange-400" />
          </div>
          <h1 className="text-3xl font-bold">Peças Sem Localização</h1>
        </div>
        <p className="text-muted-foreground">
          Gerencie peças que ainda não possuem um local de armazenamento definido
        </p>
      </div>

      {/* Alert Banner */}
      {pecas.length > 0 && (
        <div className="rounded-lg border border-orange-200 dark:border-orange-800 bg-orange-50 dark:bg-orange-950/30 p-4">
          <div className="flex items-start gap-3">
            <MapPinOff className="h-5 w-5 text-orange-600 dark:text-orange-400 mt-0.5" />
            <div className="flex-1">
              <h3 className="font-semibold text-orange-900 dark:text-orange-200">
                {pecas.length} {pecas.length === 1 ? 'peça sem localização' : 'peças sem localização'}
              </h3>
              <p className="text-sm text-orange-800 dark:text-orange-300 mt-1">
                Defina locais de armazenamento para facilitar a organização e localização das peças
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Lista de Peças */}
      <div className="rounded-lg border bg-card">
        {isLoading ? (
          <div className="flex justify-center items-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
          </div>
        ) : pecas.length === 0 ? (
          <div className="text-center py-12 px-4">
            <div className="rounded-full bg-green-100 dark:bg-green-950/40 w-16 h-16 flex items-center justify-center mx-auto mb-4">
              <Package className="h-8 w-8 text-green-600 dark:text-green-400" />
            </div>
            <h3 className="text-lg font-semibold mb-2">Todas as peças têm localização</h3>
            <p className="text-muted-foreground">
              Não há peças sem localização definida no momento.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b bg-muted/50 text-left text-sm font-medium text-muted-foreground">
                  <th className="p-4">Código</th>
                  <th className="p-4">Descrição</th>
                  <th className="p-4">Marca</th>
                  <th className="p-4 text-right">Qtd. Atual</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 text-right">Valor</th>
                  <th className="p-4">Local</th>
                  <th className="p-4"></th>
                </tr>
              </thead>
              <tbody>
                {pecas.map((peca) => {
                  const status = getStockStatus(peca.quantidadeAtual, peca.quantidadeMinima);
                  const isEditing = selectedPecaId === peca.id;

                  return (
                    <tr key={peca.id} className="border-b hover:bg-muted/50 transition-colors">
                      <td className="p-4 font-mono text-sm">{peca.codigo}</td>
                      <td className="p-4">
                        <div>
                          <p className="font-medium">{peca.descricao}</p>
                          {peca.aplicacao && (
                            <p className="text-xs text-muted-foreground truncate max-w-xs">
                              {peca.aplicacao}
                            </p>
                          )}
                        </div>
                      </td>
                      <td className="p-4 text-sm">{peca.marca || '-'}</td>
                      <td className="p-4 text-right font-medium">
                        {peca.quantidadeAtual} {UnidadeMedidaSigla[peca.unidadeMedida]}
                      </td>
                      <td className="p-4">
                        <span
                          className={`px-2 py-1 rounded-full text-xs font-medium ${status.bgColor} ${status.textColor}`}
                        >
                          {status.label}
                        </span>
                      </td>
                      <td className="p-4 text-right font-medium">
                        {formatCurrency(peca.valorVenda)}
                      </td>
                      <td className="p-4">
                        {isEditing ? (
                          <div className="min-w-[200px]">
                            <LocalArmazenamentoSelect
                              value={selectedLocalId}
                              onChange={setSelectedLocalId}
                              placeholder="Selecione o local"
                              allowEmpty={false}
                            />
                          </div>
                        ) : (
                          <span className="text-sm text-orange-600 dark:text-orange-400 font-medium">Não definido</span>
                        )}
                      </td>
                      <td className="p-4">
                        <div className="flex items-center gap-2 justify-end">
                          {isEditing ? (
                            <>
                              <Button
                                size="sm"
                                onClick={() => handleAssignLocation(peca.id)}
                                disabled={!selectedLocalId || definirLocalizacao.isPending}
                              >
                                <MapPin className="h-3 w-3 mr-1" />
                                Salvar
                              </Button>
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={() => {
                                  setSelectedPecaId(null);
                                  setSelectedLocalId('');
                                }}
                              >
                                Cancelar
                              </Button>
                            </>
                          ) : (
                            <>
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={() => setSelectedPecaId(peca.id)}
                              >
                                <MapPin className="h-3 w-3 mr-1" />
                                Definir Local
                              </Button>
                              <Link
                                to={`/estoque/${peca.id}`}
                                className="inline-flex items-center gap-1 text-sm text-primary hover:underline"
                              >
                                <ExternalLink className="h-3 w-3" />
                              </Link>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Info */}
        {data && data.totalElements > 0 && (
          <div className="border-t p-4 text-sm text-muted-foreground">
            Mostrando {data.numberOfElements} de {data.totalElements} peças
          </div>
        )}
      </div>

      {/* Help Text */}
      <div className="rounded-lg border border-blue-200 dark:border-blue-800 bg-blue-50 dark:bg-blue-950/30 p-4">
        <h3 className="font-semibold text-blue-900 dark:text-blue-200 mb-2">Dica</h3>
        <p className="text-sm text-blue-800 dark:text-blue-300">
          Você também pode definir o local de armazenamento ao editar uma peça individualmente
          ou ao criar uma nova peça.
        </p>
      </div>
    </div>
  );
};
