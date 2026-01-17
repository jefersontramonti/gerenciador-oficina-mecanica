import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Plus, Search, Wrench, Globe, Building2, Edit, Trash2, Copy } from 'lucide-react';
import { useTemplates, useTiposManutencao, useDeletarTemplate } from '../hooks/useManutencaoPreventiva';
import { showSuccess, showError } from '@/shared/utils/notifications';
import type { TemplateManutencao } from '../types';

export default function TemplatesListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [busca, setBusca] = useState(searchParams.get('busca') || '');
  const [showDeleteModal, setShowDeleteModal] = useState<string | null>(null);

  const filters = {
    tipoManutencao: searchParams.get('tipo') || undefined,
    busca: searchParams.get('busca') || undefined,
    page: parseInt(searchParams.get('page') || '0'),
    size: 20,
  };

  const { data: templatesData, isLoading, error } = useTemplates(filters);
  const { data: tiposManutencao } = useTiposManutencao();
  const deletarMutation = useDeletarTemplate();

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const params = new URLSearchParams(searchParams);
    if (busca) {
      params.set('busca', busca);
    } else {
      params.delete('busca');
    }
    params.set('page', '0');
    setSearchParams(params);
  };

  const handleFilterChange = (key: string, value: string) => {
    const params = new URLSearchParams(searchParams);
    if (value) {
      params.set(key, value);
    } else {
      params.delete(key);
    }
    params.set('page', '0');
    setSearchParams(params);
  };

  const handleDelete = async (id: string) => {
    try {
      await deletarMutation.mutateAsync(id);
      showSuccess('Template excluído com sucesso');
      setShowDeleteModal(null);
    } catch (err) {
      console.error('Erro ao deletar template:', err);
      showError('Erro ao excluir template');
    }
  };

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
            Templates de Manutenção
          </h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 hidden sm:block">
            Modelos reutilizáveis para criar planos de manutenção
          </p>
        </div>
        <Link
          to="/manutencao-preventiva/templates/novo"
          className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          <Plus className="h-4 w-4" />
          <span>Novo Template</span>
        </Link>
      </div>

      {/* Filters */}
      <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
        <div className="flex flex-col md:flex-row gap-4">
          {/* Search */}
          <form onSubmit={handleSearch} className="flex-1">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
              <input
                type="text"
                placeholder="Buscar por nome..."
                defaultValue={busca}
                onChange={(e) => setBusca(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
              />
            </div>
          </form>

          {/* Tipo Filter */}
          <select
            defaultValue={filters.tipoManutencao || ''}
            onChange={(e) => handleFilterChange('tipo', e.target.value)}
            className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
          >
            <option value="">Todos os tipos</option>
            {tiposManutencao?.map((tipo) => (
              <option key={tipo} value={tipo}>{tipo}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      ) : error ? (
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Erro ao carregar templates. Tente novamente.
        </div>
      ) : templatesData?.content.length === 0 ? (
        <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-8 text-center">
          <Wrench className="h-12 w-12 mx-auto text-gray-400 mb-4" />
          <p className="text-gray-600 dark:text-gray-400 mb-4">
            Nenhum template encontrado
          </p>
          <Link
            to="/manutencao-preventiva/templates/novo"
            className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            <Plus className="h-4 w-4" />
            Criar primeiro template
          </Link>
        </div>
      ) : (
        <>
          {/* Templates Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {templatesData?.content.map((template) => (
              <TemplateCard
                key={template.id}
                template={template}
                onDelete={() => setShowDeleteModal(template.id)}
              />
            ))}
          </div>

          {/* Pagination */}
          {templatesData && templatesData.totalPages > 1 && (
            <div className="flex justify-center gap-2">
              <button
                onClick={() => handleFilterChange('page', String(filters.page - 1))}
                disabled={templatesData.first}
                className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg disabled:opacity-50"
              >
                Anterior
              </button>
              <span className="px-4 py-2 text-gray-600 dark:text-gray-400">
                {filters.page + 1} de {templatesData.totalPages}
              </span>
              <button
                onClick={() => handleFilterChange('page', String(filters.page + 1))}
                disabled={templatesData.last}
                className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg disabled:opacity-50"
              >
                Próximo
              </button>
            </div>
          )}
        </>
      )}

      {/* Delete Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-gray-800 rounded-lg max-w-md w-full p-6">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
              Excluir Template
            </h2>
            <p className="text-gray-600 dark:text-gray-400 mb-6">
              Tem certeza que deseja excluir este template? Planos existentes baseados nele não serão afetados.
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setShowDeleteModal(null)}
                className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
              >
                Cancelar
              </button>
              <button
                onClick={() => handleDelete(showDeleteModal)}
                disabled={deletarMutation.isPending}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                {deletarMutation.isPending ? 'Excluindo...' : 'Excluir'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

interface TemplateCardProps {
  template: TemplateManutencao;
  onDelete: () => void;
}

function TemplateCard({ template, onDelete }: TemplateCardProps) {
  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4 hover:border-blue-500 transition-colors">
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          {template.global ? (
            <Globe className="h-4 w-4 text-blue-600 dark:text-blue-400" />
          ) : (
            <Building2 className="h-4 w-4 text-gray-600 dark:text-gray-400" />
          )}
          <span className="text-xs text-gray-500 dark:text-gray-400">
            {template.global ? 'Template Global' : 'Meu Template'}
          </span>
        </div>
        {!template.global && (
          <div className="flex items-center gap-1">
            <Link
              to={`/manutencao-preventiva/templates/${template.id}/editar`}
              className="p-1 text-gray-400 hover:text-blue-600 dark:hover:text-blue-400"
            >
              <Edit className="h-4 w-4" />
            </Link>
            <button
              onClick={onDelete}
              className="p-1 text-gray-400 hover:text-red-600 dark:hover:text-red-400"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        )}
      </div>

      <h3 className="font-semibold text-gray-900 dark:text-white mb-1">
        {template.nome}
      </h3>
      <p className="text-sm text-blue-600 dark:text-blue-400 mb-2">
        {template.tipoManutencao}
      </p>
      {template.descricao && (
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-3 line-clamp-2">
          {template.descricao}
        </p>
      )}

      <div className="space-y-2 text-sm">
        <div className="flex items-center justify-between text-gray-600 dark:text-gray-400">
          <span>Critério:</span>
          <span className="font-medium text-gray-900 dark:text-white capitalize">
            {template.criterio.toLowerCase().replace('_', ' e ')}
          </span>
        </div>
        {template.intervaloDias && (
          <div className="flex items-center justify-between text-gray-600 dark:text-gray-400">
            <span>Intervalo:</span>
            <span className="font-medium text-gray-900 dark:text-white">
              {template.intervaloDias} dias
            </span>
          </div>
        )}
        {template.intervaloKm && (
          <div className="flex items-center justify-between text-gray-600 dark:text-gray-400">
            <span>Intervalo KM:</span>
            <span className="font-medium text-gray-900 dark:text-white">
              {template.intervaloKm.toLocaleString()} km
            </span>
          </div>
        )}
        {template.valorEstimado && (
          <div className="flex items-center justify-between text-gray-600 dark:text-gray-400">
            <span>Valor Estimado:</span>
            <span className="font-medium text-gray-900 dark:text-white">
              R$ {template.valorEstimado.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
            </span>
          </div>
        )}
        {template.checklist && template.checklist.length > 0 && (
          <div className="flex items-center justify-between text-gray-600 dark:text-gray-400">
            <span>Checklist:</span>
            <span className="font-medium text-gray-900 dark:text-white">
              {template.checklist.length} itens
            </span>
          </div>
        )}
      </div>

      <div className="mt-4 pt-3 border-t border-gray-100 dark:border-gray-700">
        <Link
          to={`/manutencao-preventiva/novo?template=${template.id}`}
          className="flex items-center justify-center gap-2 w-full px-3 py-2 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 rounded-lg hover:bg-blue-100 dark:hover:bg-blue-900/30 text-sm font-medium"
        >
          <Copy className="h-4 w-4" />
          Usar este Template
        </Link>
      </div>
    </div>
  );
}
