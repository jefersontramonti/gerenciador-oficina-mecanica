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
  ShoppingCart,
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
import { UnidadeMedida, CategoriaPeca, CategoriaPecaLabel, type Peca, type PecaFilters } from '../types';

const ITEMS_PER_PAGE = 20;

export const PecasListPage = () => {
  const navigate = useNavigate();

  // Filtros - aplicados diretamente sem botão
  const [filters, setFilters] = useState<PecaFilters>({
    page: 0,
    size: ITEMS_PER_PAGE,
    sort: ['descricao,asc'],
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

  const handleSearch = (field: 'codigo' | 'descricao', value: string) => {
    setFilters((prev) => ({ ...prev, [field]: value || undefined, page: 0 }));
  };

  const handleFilterChange = (key: keyof PecaFilters, value: any) => {
    const filterValue = value === '' || value === null ? undefined : value;
    setFilters((prev) => ({ ...prev, [key]: filterValue, page: 0 }));
  };

  const handleLimparFiltros = () => {
    setFilters({
      page: 0,
      size: ITEMS_PER_PAGE,
      sort: ['descricao,asc'],
    });
    // Limpar os inputs de texto (eles usam defaultValue)
    const codigoInput = document.getElementById('filtro-codigo') as HTMLInputElement;
    const descricaoInput = document.getElementById('filtro-descricao') as HTMLInputElement;
    if (codigoInput) codigoInput.value = '';
    if (descricaoInput) descricaoInput.value = '';
  };

  const handlePageChange = (newPage: number) => {
    setFilters((prev) => ({ ...prev, page: newPage }));
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
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">Estoque</h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            {data?.totalElements || 0} peça(s) cadastrada(s)
          </p>
        </div>
        <div className="flex flex-col sm:flex-row gap-2 w-full sm:w-auto">
          <Link
            to="/estoque/alertas"
            className="flex items-center justify-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-4 py-2 text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            <AlertTriangle className="h-5 w-5" />
            Alertas
          </Link>
          <Link
            to="/estoque/novo"
            className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
          >
            <Plus className="h-5 w-5" />
            Nova Peça
          </Link>
        </div>
      </div>

      {/* Filtros */}
      <div className="mb-6 rounded-lg bg-white dark:bg-gray-800 p-4 shadow">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4 xl:grid-cols-7">
          {/* Código */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Código
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
              <input
                id="filtro-codigo"
                type="text"
                placeholder="Buscar por código..."
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white py-2 pl-10 pr-4 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                defaultValue={filters.codigo}
                onChange={(e) => handleSearch('codigo', e.target.value)}
              />
            </div>
          </div>

          {/* Descrição */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Descrição
            </label>
            <input
              id="filtro-descricao"
              type="text"
              placeholder="Buscar por descrição..."
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              defaultValue={filters.descricao}
              onChange={(e) => handleSearch('descricao', e.target.value)}
            />
          </div>

          {/* Marca */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Marca
            </label>
            <select
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={filters.marca || ''}
              onChange={(e) => handleFilterChange('marca', e.target.value)}
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
              Unidade
            </label>
            <select
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={filters.unidadeMedida || ''}
              onChange={(e) => handleFilterChange('unidadeMedida', e.target.value as UnidadeMedida)}
            >
              <option value="">Todas</option>
              <option value={UnidadeMedida.UNIDADE}>Unidade (UN)</option>
              <option value={UnidadeMedida.LITRO}>Litro (L)</option>
              <option value={UnidadeMedida.METRO}>Metro (M)</option>
              <option value={UnidadeMedida.QUILO}>Quilograma (KG)</option>
            </select>
          </div>

          {/* Categoria */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Categoria
            </label>
            <select
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={filters.categoria || ''}
              onChange={(e) => handleFilterChange('categoria', e.target.value as CategoriaPeca)}
            >
              <option value="">Todas</option>
              {Object.values(CategoriaPeca).map((cat) => (
                <option key={cat} value={cat}>
                  {CategoriaPecaLabel[cat]}
                </option>
              ))}
            </select>
          </div>

          {/* Status */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Status
            </label>
            <select
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={
                filters.estoqueBaixo === true
                  ? 'estoqueBaixo'
                  : filters.ativo === true
                  ? 'ativos'
                  : filters.ativo === false
                  ? 'inativos'
                  : ''
              }
              onChange={(e) => {
                const value = e.target.value;
                if (value === 'estoqueBaixo') {
                  setFilters((prev) => ({
                    ...prev,
                    ativo: undefined,
                    estoqueBaixo: true,
                    page: 0,
                  }));
                } else if (value === 'ativos') {
                  setFilters((prev) => ({
                    ...prev,
                    ativo: true,
                    estoqueBaixo: undefined,
                    page: 0,
                  }));
                } else if (value === 'inativos') {
                  setFilters((prev) => ({
                    ...prev,
                    ativo: false,
                    estoqueBaixo: undefined,
                    page: 0,
                  }));
                } else {
                  setFilters((prev) => ({
                    ...prev,
                    ativo: undefined,
                    estoqueBaixo: undefined,
                    page: 0,
                  }));
                }
              }}
            >
              <option value="">Todos</option>
              <option value="ativos">Apenas Ativos</option>
              <option value="inativos">Apenas Inativos</option>
              <option value="estoqueBaixo">Estoque Baixo</option>
            </select>
          </div>

          {/* Limpar Filtros */}
          <div className="flex items-end">
            <button
              onClick={handleLimparFiltros}
              className="flex w-full items-center justify-center gap-2 rounded-lg border border-orange-300 dark:border-orange-700 bg-orange-50 dark:bg-orange-900/20 px-4 py-2 text-orange-700 dark:text-orange-400 hover:bg-orange-100 dark:hover:bg-orange-900/30"
            >
              <FilterX className="h-4 w-4" />
              Limpar Filtros
            </button>
          </div>
        </div>
      </div>

      {/* Mobile: Card Layout */}
      <div className="space-y-3 lg:hidden">
        {isLoading ? (
          <div className="flex justify-center items-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        ) : !data?.content || data.content.length === 0 ? (
          <div className="rounded-lg bg-white dark:bg-gray-800 p-8 text-center text-gray-500 dark:text-gray-400 shadow">
            Nenhuma peça encontrada
          </div>
        ) : (
          data.content.map((peca) => (
            <div
              key={peca.id}
              className={`rounded-lg bg-white dark:bg-gray-800 p-4 shadow ${!peca.ativo ? 'opacity-50' : ''}`}
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-mono text-sm font-medium text-gray-900 dark:text-white">{peca.codigo}</span>
                    <StockBadge quantidadeAtual={peca.quantidadeAtual} quantidadeMinima={peca.quantidadeMinima} />
                    {peca.atingiuPontoPedido && (
                      <span className="inline-flex items-center gap-1 rounded-full bg-orange-100 dark:bg-orange-900/30 px-2 py-0.5 text-xs font-medium text-orange-700 dark:text-orange-400">
                        <ShoppingCart className="h-3 w-3" />
                        Repor
                      </span>
                    )}
                  </div>
                  <p className="mt-1 text-sm text-gray-700 dark:text-gray-300 truncate">{peca.nome || peca.descricao}</p>
                  {peca.marca && <p className="mt-0.5 text-xs text-gray-500 dark:text-gray-400">{peca.marca}</p>}
                </div>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon" className="shrink-0">
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
                      Entrada
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => handleMovimentar(peca, 'SAIDA')}>
                      <ArrowUpCircle className="h-4 w-4 mr-2 text-red-600" />
                      Saída
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => handleMovimentar(peca, 'AJUSTE')}>
                      <Settings className="h-4 w-4 mr-2 text-yellow-600" />
                      Ajuste
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    {peca.ativo ? (
                      <DropdownMenuItem onClick={() => handleDesativar(peca.id)} className="text-red-600">
                        <PowerOff className="h-4 w-4 mr-2" />
                        Desativar
                      </DropdownMenuItem>
                    ) : (
                      <DropdownMenuItem onClick={() => handleReativar(peca.id)} className="text-green-600">
                        <Power className="h-4 w-4 mr-2" />
                        Reativar
                      </DropdownMenuItem>
                    )}
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>

              <div className="mt-3 grid grid-cols-2 gap-3 text-sm">
                <div>
                  <span className="text-xs text-gray-500 dark:text-gray-400">Estoque</span>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {peca.quantidadeAtual} <span className="text-xs text-gray-500">/ mín: {peca.quantidadeMinima}</span>
                  </p>
                </div>
                <div>
                  <span className="text-xs text-gray-500 dark:text-gray-400">Unidade</span>
                  <div className="mt-0.5"><UnidadeMedidaBadge unidade={peca.unidadeMedida} /></div>
                </div>
                <div>
                  <span className="text-xs text-gray-500 dark:text-gray-400">Custo / Venda</span>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {formatCurrency(peca.valorCusto)} / {formatCurrency(peca.valorVenda)}
                  </p>
                </div>
                <div>
                  <span className="text-xs text-gray-500 dark:text-gray-400">Margem</span>
                  <p className={`font-medium ${peca.margemLucro >= 30 ? 'text-green-600' : peca.margemLucro >= 10 ? 'text-yellow-600' : 'text-red-600'}`}>
                    {peca.margemLucro.toFixed(1)}%
                  </p>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Desktop: Table Layout */}
      <div className="hidden lg:block rounded-lg bg-white dark:bg-gray-800 shadow">
        <Table>
          <TableHeader>
            <TableRow className="bg-gray-50 dark:bg-gray-700">
              <TableHead className="text-gray-700 dark:text-gray-300">Código</TableHead>
              <TableHead className="text-gray-700 dark:text-gray-300">Nome</TableHead>
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
                  <TableCell className="max-w-xs truncate text-gray-700 dark:text-gray-300">{peca.nome || peca.descricao}</TableCell>
                  <TableCell className="text-gray-700 dark:text-gray-300">{peca.marca || '-'}</TableCell>
                  <TableCell className="text-center">
                    <UnidadeMedidaBadge unidade={peca.unidadeMedida} />
                  </TableCell>
                  <TableCell className="text-right font-medium text-gray-900 dark:text-white">
                    {peca.quantidadeAtual}
                  </TableCell>
                  <TableCell className="text-right text-gray-700 dark:text-gray-300">{peca.quantidadeMinima}</TableCell>
                  <TableCell className="text-center">
                    <div className="flex items-center justify-center gap-1">
                      <StockBadge
                        quantidadeAtual={peca.quantidadeAtual}
                        quantidadeMinima={peca.quantidadeMinima}
                      />
                      {peca.atingiuPontoPedido && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-orange-100 dark:bg-orange-900/30 px-2 py-0.5 text-xs font-medium text-orange-700 dark:text-orange-400" title="Atingiu ponto de pedido - reabastecer!">
                          <ShoppingCart className="h-3 w-3" />
                          Repor
                        </span>
                      )}
                    </div>
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
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:justify-between sm:items-center">
          <p className="text-sm text-gray-600 dark:text-gray-400 text-center sm:text-left">
            Mostrando {data.number * data.size + 1} a{' '}
            {Math.min((data.number + 1) * data.size, data.totalElements)} de {data.totalElements}{' '}
            peças
          </p>
          <div className="flex gap-2 justify-center sm:justify-end">
            <button
              disabled={data.first}
              onClick={() => handlePageChange((filters.page || 0) - 1)}
              className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Anterior
            </button>
            <span className="hidden sm:flex items-center px-4 py-2 text-sm text-gray-700 dark:text-gray-300">
              Página {data.number + 1} de {data.totalPages}
            </span>
            <button
              disabled={data.last}
              onClick={() => handlePageChange((filters.page || 0) + 1)}
              className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
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
