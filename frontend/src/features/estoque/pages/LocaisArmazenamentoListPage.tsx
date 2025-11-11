/**
 * Página de listagem de Locais de Armazenamento
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  MapPin,
  Plus,
  Eye,
  Edit,
  Power,
  PowerOff,
  Trash2,
  MoreVertical,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
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
import {
  useLocaisArmazenamento,
  useDesativarLocal,
  useReativarLocal,
  useExcluirLocal,
} from '../hooks/useLocaisArmazenamento';
import { TipoLocalLabel, TipoLocalIcon } from '../types';

export const LocaisArmazenamentoListPage = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');

  // Queries
  const { data: locais = [], isLoading } = useLocaisArmazenamento();
  const desativarLocal = useDesativarLocal();
  const reativarLocal = useReativarLocal();
  const excluirLocal = useExcluirLocal();

  // Filtrar locais por termo de busca
  const locaisFiltrados = locais.filter((local) => {
    const term = searchTerm.toLowerCase();
    return (
      local.codigo.toLowerCase().includes(term) ||
      local.descricao.toLowerCase().includes(term) ||
      TipoLocalLabel[local.tipo].toLowerCase().includes(term)
    );
  });

  const handleDesativar = async (id: string) => {
    if (confirm('Deseja realmente desativar este local de armazenamento?')) {
      await desativarLocal.mutateAsync(id);
    }
  };

  const handleReativar = async (id: string) => {
    await reativarLocal.mutateAsync(id);
  };

  const handleExcluir = async (id: string) => {
    if (
      confirm(
        'ATENÇÃO: Deseja realmente EXCLUIR PERMANENTEMENTE este local? Esta ação não pode ser desfeita. Só é possível se não houver peças vinculadas.'
      )
    ) {
      await excluirLocal.mutateAsync(id);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
            <MapPin className="h-8 w-8" />
            Locais de Armazenamento
          </h1>
          <p className="text-muted-foreground">
            Organização física do estoque (depósitos, prateleiras, gavetas)
          </p>
        </div>
        <Button onClick={() => navigate('/estoque/locais/novo')}>
          <Plus className="h-4 w-4 mr-2" />
          Novo Local
        </Button>
      </div>

      {/* Filtros */}
      <div className="flex gap-4">
        <div className="flex-1">
          <Input
            placeholder="Buscar por código, descrição ou tipo..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-4 md:grid-cols-3">
        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">Total de Locais</p>
              <p className="text-2xl font-bold">{locais.length}</p>
            </div>
            <MapPin className="h-8 w-8 text-muted-foreground" />
          </div>
        </div>
        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">Locais Ativos</p>
              <p className="text-2xl font-bold text-green-600">
                {locais.filter((l) => l.ativo).length}
              </p>
            </div>
            <Power className="h-8 w-8 text-green-600" />
          </div>
        </div>
        <div className="rounded-lg border bg-card p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">Locais Inativos</p>
              <p className="text-2xl font-bold text-gray-400">
                {locais.filter((l) => !l.ativo).length}
              </p>
            </div>
            <PowerOff className="h-8 w-8 text-gray-400" />
          </div>
        </div>
      </div>

      {/* Tabela */}
      <div className="rounded-lg border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Código</TableHead>
              <TableHead>Descrição</TableHead>
              <TableHead>Tipo</TableHead>
              <TableHead>Local Pai</TableHead>
              <TableHead>Filhos</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="w-[100px]">Ações</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground">
                  Carregando...
                </TableCell>
              </TableRow>
            ) : locaisFiltrados.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground">
                  Nenhum local encontrado
                </TableCell>
              </TableRow>
            ) : (
              locaisFiltrados.map((local) => (
                <TableRow key={local.id}>
                  <TableCell className="font-mono">{local.codigo}</TableCell>
                  <TableCell className="font-medium">{local.descricao}</TableCell>
                  <TableCell>
                    <span className="inline-flex items-center gap-1 text-sm">
                      <span>{TipoLocalIcon[local.tipo]}</span>
                      {TipoLocalLabel[local.tipo]}
                    </span>
                  </TableCell>
                  <TableCell>
                    {local.localizacaoPai ? (
                      <span className="text-sm text-muted-foreground">
                        {local.localizacaoPai.codigo} - {local.localizacaoPai.descricao}
                      </span>
                    ) : (
                      <span className="text-sm text-muted-foreground italic">Raiz</span>
                    )}
                  </TableCell>
                  <TableCell>
                    <span className="text-sm text-muted-foreground">
                      {local.temFilhos ? 'Sim' : 'Não'}
                    </span>
                  </TableCell>
                  <TableCell>
                    {local.ativo ? (
                      <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2 py-1 text-xs font-medium text-green-700">
                        <Power className="h-3 w-3" />
                        Ativo
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2 py-1 text-xs font-medium text-gray-600">
                        <PowerOff className="h-3 w-3" />
                        Inativo
                      </span>
                    )}
                  </TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="sm">
                          <MoreVertical className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => navigate(`/estoque/locais/${local.id}`)}>
                          <Eye className="mr-2 h-4 w-4" />
                          Visualizar
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={() => navigate(`/estoque/locais/${local.id}/editar`)}
                        >
                          <Edit className="mr-2 h-4 w-4" />
                          Editar
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        {local.ativo ? (
                          <DropdownMenuItem onClick={() => handleDesativar(local.id)}>
                            <PowerOff className="mr-2 h-4 w-4" />
                            Desativar
                          </DropdownMenuItem>
                        ) : (
                          <DropdownMenuItem onClick={() => handleReativar(local.id)}>
                            <Power className="mr-2 h-4 w-4" />
                            Reativar
                          </DropdownMenuItem>
                        )}
                        <DropdownMenuSeparator />
                        <DropdownMenuItem
                          className="text-red-600"
                          onClick={() => handleExcluir(local.id)}
                        >
                          <Trash2 className="mr-2 h-4 w-4" />
                          Excluir Permanentemente
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
};
