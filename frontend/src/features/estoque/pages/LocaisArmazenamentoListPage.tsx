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
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <MapPin className="h-8 w-8 text-gray-900 dark:text-white" />
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
              Locais de Armazenamento
            </h1>
          </div>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            Organização física do estoque (depósitos, prateleiras, gavetas)
          </p>
        </div>
        <button
          onClick={() => navigate('/estoque/locais/novo')}
          className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
        >
          <Plus className="h-4 w-4" />
          Novo Local
        </button>
      </div>

      {/* Filtros */}
      <div className="mb-6">
        <input
          type="text"
          placeholder="Buscar por código, descrição ou tipo..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full max-w-md rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
        />
      </div>

      {/* Stats Cards */}
      <div className="mb-6 grid gap-4 md:grid-cols-3">
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 dark:text-gray-400">Total de Locais</p>
              <p className="text-2xl font-bold text-gray-900 dark:text-white">{locais.length}</p>
            </div>
            <MapPin className="h-8 w-8 text-gray-400 dark:text-gray-500" />
          </div>
        </div>
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 dark:text-gray-400">Locais Ativos</p>
              <p className="text-2xl font-bold text-green-600 dark:text-green-400">
                {locais.filter((l) => l.ativo).length}
              </p>
            </div>
            <Power className="h-8 w-8 text-green-600 dark:text-green-400" />
          </div>
        </div>
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 dark:text-gray-400">Locais Inativos</p>
              <p className="text-2xl font-bold text-gray-500 dark:text-gray-400">
                {locais.filter((l) => !l.ativo).length}
              </p>
            </div>
            <PowerOff className="h-8 w-8 text-gray-400 dark:text-gray-500" />
          </div>
        </div>
      </div>

      {/* Tabela */}
      <div className="rounded-lg bg-white dark:bg-gray-800 shadow overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead className="bg-gray-50 dark:bg-gray-700">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Código
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Descrição
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Tipo
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Local Pai
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Filhos
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Status
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300 w-[100px]">
                  Ações
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-800">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                    <div className="flex justify-center">
                      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                    </div>
                  </td>
                </tr>
              ) : locaisFiltrados.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                    Nenhum local encontrado
                  </td>
                </tr>
              ) : (
                locaisFiltrados.map((local) => (
                  <tr key={local.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                    <td className="px-6 py-4 text-sm font-mono text-gray-900 dark:text-white">
                      {local.codigo}
                    </td>
                    <td className="px-6 py-4 text-sm font-medium text-gray-900 dark:text-white">
                      {local.descricao}
                    </td>
                    <td className="px-6 py-4">
                      <span className="inline-flex items-center gap-1 text-sm text-gray-900 dark:text-white">
                        <span>{TipoLocalIcon[local.tipo]}</span>
                        {TipoLocalLabel[local.tipo]}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      {local.localizacaoPai ? (
                        <span className="text-sm text-gray-600 dark:text-gray-400">
                          {local.localizacaoPai.codigo} - {local.localizacaoPai.descricao}
                        </span>
                      ) : (
                        <span className="text-sm text-gray-500 dark:text-gray-400 italic">Raiz</span>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-sm text-gray-600 dark:text-gray-400">
                        {local.temFilhos ? 'Sim' : 'Não'}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      {local.ativo ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-green-100 dark:bg-green-900/30 px-2 py-1 text-xs font-medium text-green-700 dark:text-green-300">
                          <Power className="h-3 w-3" />
                          Ativo
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 dark:bg-gray-700 px-2 py-1 text-xs font-medium text-gray-600 dark:text-gray-300">
                          <PowerOff className="h-3 w-3" />
                          Inativo
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <button className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 dark:text-gray-400">
                            <MoreVertical className="h-4 w-4" />
                          </button>
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
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
