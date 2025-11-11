/**
 * Página de listagem de Peças/Estoque
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Package,
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
} from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Label } from '@/shared/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select';
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
import { Checkbox } from '@/shared/components/ui/checkbox';
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

    // Filtros booleanos: enviar para API apenas se true
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
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
            <Package className="h-8 w-8" />
            Gerenciamento de Estoque
          </h1>
          <p className="text-muted-foreground">Controle de peças e inventário</p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={() => navigate('/estoque/alertas')}
            className="flex items-center gap-2"
          >
            <AlertTriangle className="h-4 w-4" />
            Alertas
          </Button>
          <Button onClick={() => navigate('/estoque/novo')}>
            <Plus className="h-4 w-4 mr-2" />
            Nova Peça
          </Button>
        </div>
      </div>

      {/* Filtros */}
      <div className="bg-card p-4 rounded-lg border space-y-4">
        <h3 className="font-medium">Filtros</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="space-y-2">
            <Label>Código</Label>
            <Input
              placeholder="Buscar por código..."
              value={localFilters.codigo}
              onChange={(e) =>
                setLocalFilters((prev) => ({ ...prev, codigo: e.target.value }))
              }
            />
          </div>

          <div className="space-y-2">
            <Label>Descrição</Label>
            <Input
              placeholder="Buscar por descrição..."
              value={localFilters.descricao}
              onChange={(e) =>
                setLocalFilters((prev) => ({ ...prev, descricao: e.target.value }))
              }
            />
          </div>

          <div className="space-y-2">
            <Label>Marca</Label>
            <Select
              value={localFilters.marca}
              onValueChange={(value) =>
                setLocalFilters((prev) => ({ ...prev, marca: value }))
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="Todas as marcas" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">Todas as marcas</SelectItem>
                {marcas?.map((marca) => (
                  <SelectItem key={marca} value={marca}>
                    {marca}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>Unidade de Medida</Label>
            <Select
              value={localFilters.unidadeMedida}
              onValueChange={(value) =>
                setLocalFilters((prev) => ({ ...prev, unidadeMedida: value }))
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="Todas" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">Todas</SelectItem>
                <SelectItem value={UnidadeMedida.UNIDADE}>Unidade (UN)</SelectItem>
                <SelectItem value={UnidadeMedida.LITRO}>Litro (L)</SelectItem>
                <SelectItem value={UnidadeMedida.METRO}>Metro (M)</SelectItem>
                <SelectItem value={UnidadeMedida.QUILO}>Quilograma (KG)</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        <div className="flex gap-4">
          <div className="flex items-center space-x-2">
            <Checkbox
              id="apenasAtivos"
              checked={localFilters.apenasAtivos}
              onChange={(e) => {
                const isChecked = e.target.checked;
                setLocalFilters((prev) => ({
                  ...prev,
                  apenasAtivos: isChecked,
                  // Se marcar "Apenas Ativos", desmarca "Estoque Baixo"
                  apenasEstoqueBaixo: isChecked ? false : prev.apenasEstoqueBaixo
                }));
              }}
            />
            <label
              htmlFor="apenasAtivos"
              className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 cursor-pointer"
            >
              Apenas ativos
            </label>
          </div>

          <div className="flex items-center space-x-2">
            <Checkbox
              id="apenasEstoqueBaixo"
              checked={localFilters.apenasEstoqueBaixo}
              onChange={(e) => {
                const isChecked = e.target.checked;
                setLocalFilters((prev) => ({
                  ...prev,
                  apenasEstoqueBaixo: isChecked,
                  // Se marcar "Estoque Baixo", desmarca "Apenas Ativos"
                  apenasAtivos: isChecked ? false : prev.apenasAtivos
                }));
              }}
            />
            <label
              htmlFor="apenasEstoqueBaixo"
              className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 cursor-pointer"
            >
              Apenas estoque baixo
            </label>
          </div>
        </div>

        <div className="flex gap-2">
          <Button onClick={handleAplicarFiltros}>Aplicar Filtros</Button>
          <Button variant="outline" onClick={handleLimparFiltros}>
            Limpar Filtros
          </Button>
        </div>
      </div>

      {/* Tabela */}
      <div className="bg-card rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Código</TableHead>
              <TableHead>Descrição</TableHead>
              <TableHead>Marca</TableHead>
              <TableHead className="text-center">Unidade</TableHead>
              <TableHead className="text-right">Qtd Atual</TableHead>
              <TableHead className="text-right">Qtd Mín</TableHead>
              <TableHead className="text-center">Status</TableHead>
              <TableHead className="text-right">Valor Custo</TableHead>
              <TableHead className="text-right">Valor Venda</TableHead>
              <TableHead className="text-right">Margem %</TableHead>
              <TableHead className="text-right">Ações</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={11} className="text-center py-12">
                  <div className="flex justify-center items-center">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
                  </div>
                </TableCell>
              </TableRow>
            ) : !data?.content || data.content.length === 0 ? (
              <TableRow>
                <TableCell colSpan={11} className="text-center py-12 text-muted-foreground">
                  Nenhuma peça encontrada
                </TableCell>
              </TableRow>
            ) : (
              data.content.map((peca) => (
                <TableRow key={peca.id} className={!peca.ativo ? 'opacity-50' : ''}>
                  <TableCell className="font-medium">{peca.codigo}</TableCell>
                  <TableCell className="max-w-xs truncate">{peca.descricao}</TableCell>
                  <TableCell>{peca.marca || '-'}</TableCell>
                  <TableCell className="text-center">
                    <UnidadeMedidaBadge unidade={peca.unidadeMedida} />
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    {peca.quantidadeAtual}
                  </TableCell>
                  <TableCell className="text-right">{peca.quantidadeMinima}</TableCell>
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
                        <DropdownMenuItem
                          onClick={() => navigate(`/estoque/${peca.id}/editar`)}
                        >
                          <Edit className="h-4 w-4 mr-2" />
                          Editar
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem
                          onClick={() => handleMovimentar(peca, 'ENTRADA')}
                        >
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

      {/* Paginação (simplificada) */}
      {data && data.totalPages > 1 && (
        <div className="flex justify-between items-center">
          <p className="text-sm text-muted-foreground">
            Mostrando {data.content.length} de {data.totalElements} peças
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={data.first}
              onClick={() => setFilters((prev) => ({ ...prev, page: (prev.page || 0) - 1 }))}
            >
              Anterior
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={data.last}
              onClick={() => setFilters((prev) => ({ ...prev, page: (prev.page || 0) + 1 }))}
            >
              Próxima
            </Button>
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
