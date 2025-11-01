/**
 * Tabela de itens da Ordem de Serviço
 * Exibe peças e serviços com suporte para modo leitura e edição
 */

import { Wrench, Package, Trash2, Edit } from 'lucide-react';
import { TipoItem, type ItemOS } from '../types';

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

export const ItemOSTable: React.FC<ItemOSTableProps> = ({
  items,
  onEdit,
  onDelete,
  readOnly = false,
}) => {
  if (items.length === 0) {
    return (
      <div className="rounded-lg border border-gray-200 bg-gray-50 p-8 text-center">
        <Package className="mx-auto h-12 w-12 text-gray-400" />
        <p className="mt-2 text-sm text-gray-600">Nenhum item adicionado</p>
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                Tipo
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                Descrição
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700">
                Qtd.
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700">
                Valor Unit.
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700">
                Desconto
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700">
                Valor Total
              </th>
              {!readOnly && (onEdit || onDelete) && (
                <th className="px-4 py-3 text-center text-xs font-medium uppercase tracking-wider text-gray-700">
                  Ações
                </th>
              )}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 bg-white">
            {items.map((item, index) => (
              <tr key={index} className="hover:bg-gray-50">
                {/* Tipo */}
                <td className="whitespace-nowrap px-4 py-3">
                  <div className="flex items-center gap-2">
                    {item.tipo === TipoItem.PECA ? (
                      <Package className="h-4 w-4 text-blue-600" />
                    ) : (
                      <Wrench className="h-4 w-4 text-green-600" />
                    )}
                    <span className="text-sm font-medium text-gray-900">
                      {item.tipo === TipoItem.PECA ? 'Peça' : 'Serviço'}
                    </span>
                  </div>
                </td>

                {/* Descrição */}
                <td className="px-4 py-3">
                  <p className="text-sm text-gray-900">{item.descricao}</p>
                </td>

                {/* Quantidade */}
                <td className="whitespace-nowrap px-4 py-3 text-right text-sm text-gray-900">
                  {item.quantidade}
                </td>

                {/* Valor Unitário */}
                <td className="whitespace-nowrap px-4 py-3 text-right text-sm text-gray-900">
                  {formatCurrency(item.valorUnitario)}
                </td>

                {/* Desconto */}
                <td className="whitespace-nowrap px-4 py-3 text-right text-sm text-gray-900">
                  {item.desconto > 0 ? formatCurrency(item.desconto) : '-'}
                </td>

                {/* Valor Total */}
                <td className="whitespace-nowrap px-4 py-3 text-right text-sm font-medium text-gray-900">
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
                          className="rounded p-1 text-blue-600 hover:bg-blue-50"
                          title="Editar item"
                        >
                          <Edit className="h-4 w-4" />
                        </button>
                      )}
                      {onDelete && (
                        <button
                          type="button"
                          onClick={() => onDelete(index)}
                          className="rounded p-1 text-red-600 hover:bg-red-50"
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
          <tfoot className="bg-gray-50">
            <tr>
              <td colSpan={5} className="px-4 py-3 text-right text-sm font-medium text-gray-700">
                Total Geral:
              </td>
              <td className="whitespace-nowrap px-4 py-3 text-right text-sm font-bold text-gray-900">
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
