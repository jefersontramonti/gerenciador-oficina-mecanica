/**
 * Página de listagem de Peças/Estoque
 */

import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Plus,
  AlertTriangle,
  Eye,
  Edit,
  Power,
  PowerOff,
  MoreVertical,
  ArrowDownCircle,
  ArrowUpCircle,
  Settings,
  Search,
  FilterX,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/shared/components/ui/table';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/shared/components/ui/dropdown-menu';
import { formatCurrency } from '@/shared/utils/formatters';
import { usePecas, useMarcas, useDesativarPeca, useReativarPeca } from '../hooks/usePecas';
import { StockBadge, UnidadeMedidaBadge, MovimentacaoModal } from '../components';
import { UnidadeMedida, type Peca, type PecaFilters } from '../types';

export const PecasListPage = () => {
  const navigate = useNavigate();

  // Filtros
  const [filters, setFilters] = useState<PecaFilters>({
    page: 0,
    size: 20,
    sort: ['descricao,asc'],
  });

  const [localFilters, setLocalFilters] = useState({
    codigo: '',
    descricao: '',
    marca: '',
    unidadeMedida: '',
    apenasAtivos: false,
    apenasEstoqueBaixo: false,
  });

  // Queries
  const { data, isLoading } = usePecas(filters);
  const { data: marcas } = useMarcas();
  const desativarPeca = useDesativarPeca();
  const reativarPeca = useReativarPeca();

  // Modal de movimentação
  const [movimentacaoModal, setMovimentacaoModal] = useState<{
    isOpen: boolean;
    tipo: 'ENTRADA' | 'SAIDA' | 'AJUSTE';
    peca: Peca | null;
  }>({
    isOpen: false,
    tipo: 'ENTRADA',
    peca: null,
  });

  const handleAplicarFiltros = () => {
    const newFilters: PecaFilters = {
      page: 0,
      size: 20,
      sort: ['descricao,asc'],
    };

    if (localFilters.codigo) newFilters.codigo = localFilters.codigo;
    if (localFilters.descricao) newFilters.descricao = localFilters.descricao;
    if (localFilters.marca) newFilters.marca = localFilters.marca;
    if (localFilters.unidadeMedida)
      newFilters.unidadeMedida = localFilters.unidadeMedida as UnidadeMedida;

    if (localFilters.apenasAtivos) newFilters.ativo = true;
    if (localFilters.apenasEstoqueBaixo) newFilters.estoqueBaixo = true;

    setFilters(newFilters);
  };

  const handleLimparFiltros = () => {
    setLocalFilters({
      codigo: '',
      descricao: '',
      marca: '',
      unidadeMedida: '',
      apenasAtivos: false,
      apenasEstoqueBaixo: false,
    });
    setFilters({
      page: 0,
      size: 20,
      sort: ['descricao,asc'],
    });
  };

  const handleDesativar = async (id: string) => {
    if (confirm('Deseja realmente desativar esta peça?')) {
      await desativarPeca.mutateAsync(id);
    }
  };

  const handleReativar = async (id: string) => {
    await reativarPeca.mutateAsync(id);
  };

  const handleMovimentar = (peca: Peca, tipo: 'ENTRADA' | 'SAIDA' | 'AJUSTE') => {
    setMovimentacaoModal({
      isOpen: true,
      tipo,
      peca,
    });
  };

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Estoque</h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            {data?.totalElements || 0} peça(s) cadastrada(s)
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            to="/estoque/alertas"
            className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-4 py-2 text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            <AlertTriangle className="h-5 w-5" />
            Alertas
          </Link>
          <Link
            to="/estoque/novo"
            className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
          >
            <Plus className="h-5 w-5" />
            Nova Peça
          </Link>
        </div>
      </div>

      {/* Filtros */}
      <div className="mb-6 rounded-lg bg-white dark:bg-gray-800 p-4 shadow">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {/* Código */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Código
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Buscar por código..."
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white py-2 pl-10 pr-4 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                value={localFilters.codigo}
                onChange={(e) =>
                  setLocalFilters((prev) => ({ ...prev, codigo: e.target.value }))
                }
              />
            </div>
          </div>

          {/* Descrição */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Descrição
            </label>
            <input
              type="text"
              placeholder="Buscar por descrição..."
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={localFilters.descricao}
              onChange={(e) =>
                setLocalFilters((prev) => ({ ...prev, descricao: e.target.value }))
              }
            />
          </div>

          {/* Marca */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Marca
            </label>
            <select
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={localFilters.marca}
              onChange={(e) =>
                setLocalFilters((prev) => ({ ...prev, marca: e.target.value }))
              }
            >
              <option value="">Todas as marcas</option>
              {marcas?.map((marca) => (
                <option key={marca} value={marca}>
                  {marca}
                </option>
              ))}
            </select>
          </div>

          {/* Unidade de Medida */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Unidade de Medida
            </label>
            <select
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={localFilters.unidadeMedida}
              onChange={(e) =>
                setLocalFilters((prev) => ({ ...prev, unidadeMedida: e.target.value }))
              }
            >
              <option value="">Todas</option>
              <option value={UnidadeMedida.UNIDADE}>Unidade (UN)</option>
              <option value={UnidadeMedida.LITRO}>Litro (L)</option>
              <option value={UnidadeMedida.METRO}>Metro (M)</option>
              <option value={UnidadeMedida.QUILO}>Quilograma (KG)</option>
            </select>
          </div>
        </div>

        {/* Checkboxes e botões */}
        <div className="mt-4 flex flex-wrap items-center gap-6">
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="apenasAtivos"
              className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              checked={localFilters.apenasAtivos}
              onChange={(e) => {
                setLocalFilters((prev) => ({
                  ...prev,
                  apenasAtivos: e.target.checked,
                  apenasEstoqueBaixo: e.target.checked ? false : prev.apenasEstoqueBaixo,
                }));
              }}
            />
            <label
              htmlFor="apenasAtivos"
              className="text-sm text-gray-700 dark:text-gray-300 cursor-pointer"
            >
              Apenas ativos
            </label>
          </div>

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="apenasEstoqueBaixo"
              className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              checked={localFilters.apenasEstoqueBaixo}
              onChange={(e) => {
                setLocalFilters((prev) => ({
                  ...prev,
                  apenasEstoqueBaixo: e.target.checked,
                  apenasAtivos: e.target.checked ? false : prev.apenasAtivos,
                }));
              }}
            />
            <label
              htmlFor="apenasEstoqueBaixo"
              className="text-sm text-gray-700 dark:text-gray-300 cursor-pointer"
            >
              Apenas estoque baixo
            </label>
          </div>

          <div className="flex gap-2 ml-auto">
            <button
              onClick={handleAplicarFiltros}
              className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
            >
              <Search className="h-4 w-4" />
              Aplicar Filtros
            </button>
            <button
              onClick={handleLimparFiltros}
              className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-4 py-2 text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              <FilterX className="h-4 w-4" />
              Limpar
            </button>
          </div>
        </div>
      </div>

      {/* Tabela */}
      <div className="rounded-lg bg-white dark:bg-gray-800 shadow overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow className="bg-gray-50 dark:bg-gray-700">
              <TableHead className="text-gray-700 dark:text-gray-300">Código</TableHead>
              <TableHead className="text-gray-700 dark:text-gray-300">Descrição</TableHead>
              <TableHead className="text-gray-700 dark:text-gray-300">Marca</TableHead>
              <TableHead className="text-center text-gray-700 dark:text-gray-300">Unidade</TableHead>
              <TableHead className="text-right text-gray-700 dark:text-gray-300">Qtd Atual</TableHead>
              <TableHead className="text-right text-gray-700 dark:text-gray-300">Qtd Mín</TableHead>
              <TableHead className="text-center text-gray-700 dark:text-gray-300">Status</TableHead>
              <TableHead className="text-right text-gray-700 dark:text-gray-300">Valor Custo</TableHead>
              <TableHead className="text-right text-gray-700 dark:text-gray-300">Valor Venda</TableHead>
              <TableHead className="text-right text-gray-700 dark:text-gray-300">Margem %</TableHead>
              <TableHead className="text-right text-gray-700 dark:text-gray-300">Ações</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={11} className="text-center py-12">
                  <div className="flex justify-center items-center">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                  </div>
                </TableCell>
              </TableRow>
            ) : !data?.content || data.content.length === 0 ? (
              <TableRow>
                <TableCell colSpan={11} className="text-center py-12 text-gray-500 dark:text-gray-400">
                  Nenhuma peça encontrada
                </TableCell>
              </TableRow>
            ) : (
              data.content.map((peca) => (
                <TableRow key={peca.id} className={!peca.ativo ? 'opacity-50' : ''}>
                  <TableCell className="font-medium text-gray-900 dark:text-white">{peca.codigo}</TableCell>
                  <TableCell className="max-w-xs truncate text-gray-700 dark:text-gray-300">{peca.descricao}</TableCell>
                  <TableCell className="text-gray-700 dark:text-gray-300">{peca.marca || '-'}</TableCell>
                  <TableCell className="text-center">
                    <UnidadeMedidaBadge unidade={peca.unidadeMedida} />
                  </TableCell>
                  <TableCell className="text-right font-medium text-gray-900 dark:text-white">
                    {peca.quantidadeAtual}
                  </TableCell>
                  <TableCell className="text-right text-gray-700 dark:text-gray-300">{peca.quantidadeMinima}</TableCell>
                  <TableCell className="text-center">
                    <StockBadge
                      quantidadeAtual={peca.quantidadeAtual}
                      quantidadeMinima={peca.quantidadeMinima}
                    />
                  </TableCell>
                  <TableCell className="text-right text-gray-700 dark:text-gray-300">
                    {formatCurrency(peca.valorCusto)}
                  </TableCell>
                  <TableCell className="text-right text-gray-700 dark:text-gray-300">
                    {formatCurrency(peca.valorVenda)}
                  </TableCell>
                  <TableCell className="text-right">
                    <span
                      className={
                        peca.margemLucro >= 30
                          ? 'text-green-600 font-medium'
                          : peca.margemLucro >= 10
                          ? 'text-yellow-600 font-medium'
                          : 'text-red-600 font-medium'
                      }
                    >
                      {peca.margemLucro.toFixed(1)}%
                    </span>
                  </TableCell>
                  <TableCell className="text-right">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreVertical className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => navigate(`/estoque/${peca.id}`)}>
                          <Eye className="h-4 w-4 mr-2" />
                          Ver Detalhes
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => navigate(`/estoque/${peca.id}/editar`)}>
                          <Edit className="h-4 w-4 mr-2" />
                          Editar
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem onClick={() => handleMovimentar(peca, 'ENTRADA')}>
                          <ArrowDownCircle className="h-4 w-4 mr-2 text-green-600" />
                          Registrar Entrada
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => handleMovimentar(peca, 'SAIDA')}>
                          <ArrowUpCircle className="h-4 w-4 mr-2 text-red-600" />
                          Registrar Saída
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => handleMovimentar(peca, 'AJUSTE')}>
                          <Settings className="h-4 w-4 mr-2 text-yellow-600" />
                          Ajustar Inventário
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        {peca.ativo ? (
                          <DropdownMenuItem
                            onClick={() => handleDesativar(peca.id)}
                            className="text-red-600"
                          >
                            <PowerOff className="h-4 w-4 mr-2" />
                            Desativar
                          </DropdownMenuItem>
                        ) : (
                          <DropdownMenuItem
                            onClick={() => handleReativar(peca.id)}
                            className="text-green-600"
                          >
                            <Power className="h-4 w-4 mr-2" />
                            Reativar
                          </DropdownMenuItem>
                        )}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Paginação */}
      {data && data.totalPages > 1 && (
        <div className="mt-4 flex justify-between items-center">
          <p className="text-sm text-gray-600 dark:text-gray-400">
            Mostrando {data.content.length} de {data.totalElements} peças
          </p>
          <div className="flex gap-2">
            <button
              disabled={data.first}
              onClick={() => setFilters((prev) => ({ ...prev, page: (prev.page || 0) - 1 }))}
              className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Anterior
            </button>
            <button
              disabled={data.last}
              onClick={() => setFilters((prev) => ({ ...prev, page: (prev.page || 0) + 1 }))}
              className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Próxima
            </button>
          </div>
        </div>
      )}

      {/* Modal de Movimentação */}
      {movimentacaoModal.peca && (
        <MovimentacaoModal
          isOpen={movimentacaoModal.isOpen}
          onClose={() =>
            setMovimentacaoModal({ isOpen: false, tipo: 'ENTRADA', peca: null })
          }
          tipo={movimentacaoModal.tipo}
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
