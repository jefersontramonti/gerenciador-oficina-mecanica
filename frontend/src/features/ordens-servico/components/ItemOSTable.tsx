/**
 * Tabela de itens da Ordem de Serviço
 * Exibe peças e serviços com suporte para modo leitura e edição
 */

import { Wrench, Package, Trash2, Edit, Warehouse, ShoppingBag, UserCheck } from 'lucide-react';
import { TipoItem, OrigemPeca, type ItemOS } from '../types';

interface ItemOSTableProps {
  items: ItemOS[];
  onEdit?: (index: number) => void;
  onDelete?: (index: number) => void;
  readOnly?: boolean;
}

/**
 * Formata valor monetário
 */
const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

/**
 * Configuração de exibição para cada origem de peça
 */
const origemPecaConfig: Record<OrigemPeca, { label: string; icon: React.ReactNode; className: string }> = {
  [OrigemPeca.ESTOQUE]: {
    label: 'Estoque',
    icon: <Warehouse className="h-3 w-3" />,
    className: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  },
  [OrigemPeca.AVULSA]: {
    label: 'Avulsa',
    icon: <ShoppingBag className="h-3 w-3" />,
    className: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  },
  [OrigemPeca.CLIENTE]: {
    label: 'Cliente',
    icon: <UserCheck className="h-3 w-3" />,
    className: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
  },
};

/**
 * Renderiza badge de origem da peça
 */
const OrigemPecaBadge: React.FC<{ origem?: OrigemPeca }> = ({ origem }) => {
  if (!origem) return null;

  const config = origemPecaConfig[origem];
  if (!config) return null;

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${config.className}`}
      title={`Origem: ${config.label}`}
    >
      {config.icon}
      {config.label}
    </span>
  );
};

export const ItemOSTable: React.FC<ItemOSTableProps> = ({
  items,
  onEdit,
  onDelete,
  readOnly = false,
}) => {
  if (items.length === 0) {
    return (
      <div className="rounded-lg border border-gray-200 bg-gray-50 p-8 text-center dark:border-gray-700 dark:bg-gray-900">
        <Package className="mx-auto h-12 w-12 text-gray-400 dark:text-gray-500" />
        <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">Nenhum item adicionado</p>
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm dark:border-gray-700 dark:bg-gray-800">
      <div className="overflow-x-auto">
        <table className="w-full divide-y divide-gray-200 dark:divide-gray-700">
          <thead className="bg-gray-50 dark:bg-gray-900">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                Tipo
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                Descrição
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                Qtd.
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                Valor Unit.
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                Desconto
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                Valor Total
              </th>
              {!readOnly && (onEdit || onDelete) && (
                <th className="px-4 py-3 text-center text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Ações
                </th>
              )}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 bg-white dark:divide-gray-700 dark:bg-gray-800">
            {items.map((item, index) => (
              <tr key={index} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                {/* Tipo */}
                <td className="whitespace-nowrap px-4 py-3">
                  <div className="flex items-center gap-2">
                    {item.tipo === TipoItem.PECA ? (
                      <Package className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                    ) : (
                      <Wrench className="h-4 w-4 text-green-600 dark:text-green-400" />
                    )}
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {item.tipo === TipoItem.PECA ? 'Peça' : 'Serviço'}
                    </span>
                  </div>
                </td>

                {/* Descrição */}
                <td className="px-4 py-3">
                  <div className="flex flex-col gap-1">
                    <p className="text-sm text-gray-900 dark:text-white">{item.descricao}</p>
                    {item.tipo === TipoItem.PECA && item.origemPeca && (
                      <OrigemPecaBadge origem={item.origemPeca} />
                    )}
                  </div>
                </td>

                {/* Quantidade */}
                <td className="whitespace-nowrap px-4 py-3 text-right text-sm text-gray-900 dark:text-white">
                  {item.quantidade}
                </td>

                {/* Valor Unitário */}
                <td className="whitespace-nowrap px-4 py-3 text-right text-sm text-gray-900 dark:text-white">
                  {formatCurrency(item.valorUnitario)}
                </td>

                {/* Desconto */}
                <td className="whitespace-nowrap px-4 py-3 text-right text-sm text-gray-900 dark:text-white">
                  {item.desconto > 0 ? formatCurrency(item.desconto) : '-'}
                </td>

                {/* Valor Total */}
                <td className="whitespace-nowrap px-4 py-3 text-right text-sm font-medium text-gray-900 dark:text-white">
                  {formatCurrency(item.valorTotal)}
                </td>

                {/* Ações */}
                {!readOnly && (onEdit || onDelete) && (
                  <td className="whitespace-nowrap px-4 py-3 text-center">
                    <div className="flex items-center justify-center gap-2">
                      {onEdit && (
                        <button
                          type="button"
                          onClick={() => onEdit(index)}
                          className="rounded p-1 text-blue-600 hover:bg-blue-50 dark:text-blue-400 dark:hover:bg-blue-900/30"
                          title="Editar item"
                        >
                          <Edit className="h-4 w-4" />
                        </button>
                      )}
                      {onDelete && (
                        <button
                          type="button"
                          onClick={() => onDelete(index)}
                          className="rounded p-1 text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-900/30"
                          title="Remover item"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      )}
                    </div>
                  </td>
                )}
              </tr>
            ))}
          </tbody>

          {/* Footer com totais */}
          <tfoot className="bg-gray-50 dark:bg-gray-900">
            <tr>
              <td colSpan={5} className="px-4 py-3 text-right text-sm font-medium text-gray-700 dark:text-gray-300">
                Total Geral:
              </td>
              <td className="whitespace-nowrap px-4 py-3 text-right text-sm font-bold text-gray-900 dark:text-white">
                {formatCurrency(items.reduce((sum, item) => sum + item.valorTotal, 0))}
              </td>
              {!readOnly && (onEdit || onDelete) && <td />}
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
};
