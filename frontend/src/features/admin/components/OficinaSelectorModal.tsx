/**
 * Modal para selecionar oficinas (Beta Testers)
 * Usado para adicionar oficinas específicas a uma Feature Flag
 */

import { useState, useEffect, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search, X, Building2, Check, Loader2 } from 'lucide-react';
import { oficinasService } from '../services/saasService';
import { planoLabels, type PlanoAssinatura } from '../types';

interface OficinaSelectorModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (selectedIds: string[]) => void;
  initialSelectedIds?: string[];
  title?: string;
  description?: string;
}

// Badge de plano com cores
const PlanoBadge = ({ plano }: { plano: PlanoAssinatura }) => {
  const colors: Record<PlanoAssinatura, string> = {
    ECONOMICO: 'bg-gray-100 text-gray-700 border-gray-300',
    PROFISSIONAL: 'bg-blue-100 text-blue-700 border-blue-300',
    TURBINADO: 'bg-purple-100 text-purple-700 border-purple-300',
  };

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium border ${colors[plano]}`}>
      {planoLabels[plano]}
    </span>
  );
};

export const OficinaSelectorModal = ({
  isOpen,
  onClose,
  onConfirm,
  initialSelectedIds = [],
  title = 'Selecionar Oficinas',
  description = 'Selecione as oficinas que terão acesso a esta feature (Beta Testers)',
}: OficinaSelectorModalProps) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set(initialSelectedIds));
  const [page, setPage] = useState(0);

  // Reset state when modal opens
  useEffect(() => {
    if (isOpen) {
      setSelectedIds(new Set(initialSelectedIds));
      setSearchTerm('');
      setPage(0);
    }
  }, [isOpen, initialSelectedIds]);

  // Query para buscar oficinas
  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['oficinas-selector', searchTerm, page],
    queryFn: () => oficinasService.findAll({
      searchTerm: searchTerm || undefined,
      status: 'ATIVA', // Apenas oficinas ativas
      page,
      size: 10,
    }),
    enabled: isOpen,
    staleTime: 30000,
  });

  const oficinas = data?.content || [];
  const totalPages = data?.totalPages || 0;
  const totalElements = data?.totalElements || 0;

  // Toggle seleção de uma oficina
  const toggleOficina = useCallback((id: string) => {
    setSelectedIds(prev => {
      const newSet = new Set(prev);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  }, []);

  // Selecionar/deselecionar todas da página
  const toggleAll = useCallback(() => {
    const allSelected = oficinas.every(o => selectedIds.has(o.id));
    setSelectedIds(prev => {
      const newSet = new Set(prev);
      oficinas.forEach(o => {
        if (allSelected) {
          newSet.delete(o.id);
        } else {
          newSet.add(o.id);
        }
      });
      return newSet;
    });
  }, [oficinas, selectedIds]);

  // Confirmar seleção
  const handleConfirm = () => {
    onConfirm(Array.from(selectedIds));
    onClose();
  };

  // Fechar com ESC
  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleEsc);
    return () => window.removeEventListener('keydown', handleEsc);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const allPageSelected = oficinas.length > 0 && oficinas.every(o => selectedIds.has(o.id));

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black bg-opacity-50 transition-opacity"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="flex min-h-full items-center justify-center p-4">
        <div
          className="relative w-full max-w-2xl transform rounded-lg bg-white shadow-xl transition-all"
          onClick={e => e.stopPropagation()}
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b px-6 py-4">
            <div>
              <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
              <p className="mt-1 text-sm text-gray-500">{description}</p>
            </div>
            <button
              onClick={onClose}
              className="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Search */}
          <div className="border-b px-6 py-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Buscar por nome ou CNPJ..."
                value={searchTerm}
                onChange={e => {
                  setSearchTerm(e.target.value);
                  setPage(0);
                }}
                className="w-full rounded-lg border border-gray-300 py-2 pl-10 pr-4 text-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
              {isFetching && (
                <Loader2 className="absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-gray-400" />
              )}
            </div>

            {/* Info e toggle all */}
            <div className="mt-3 flex items-center justify-between text-sm">
              <span className="text-gray-500">
                {totalElements} oficina(s) encontrada(s) | {selectedIds.size} selecionada(s)
              </span>
              {oficinas.length > 0 && (
                <button
                  onClick={toggleAll}
                  className="text-blue-600 hover:text-blue-700"
                >
                  {allPageSelected ? 'Desmarcar página' : 'Selecionar página'}
                </button>
              )}
            </div>
          </div>

          {/* Lista */}
          <div className="max-h-80 overflow-y-auto px-6 py-4">
            {isLoading ? (
              <div className="flex items-center justify-center py-12">
                <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
              </div>
            ) : oficinas.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-gray-500">
                <Building2 className="h-12 w-12 text-gray-300" />
                <p className="mt-2">Nenhuma oficina encontrada</p>
                {searchTerm && (
                  <button
                    onClick={() => setSearchTerm('')}
                    className="mt-2 text-sm text-blue-600 hover:text-blue-700"
                  >
                    Limpar busca
                  </button>
                )}
              </div>
            ) : (
              <div className="space-y-2">
                {oficinas.map(oficina => {
                  const isSelected = selectedIds.has(oficina.id);
                  return (
                    <button
                      key={oficina.id}
                      onClick={() => toggleOficina(oficina.id)}
                      className={`flex w-full items-center gap-4 rounded-lg border p-3 text-left transition-colors ${
                        isSelected
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-gray-200 bg-white hover:bg-gray-50'
                      }`}
                    >
                      {/* Checkbox */}
                      <div className={`flex h-5 w-5 items-center justify-center rounded border ${
                        isSelected
                          ? 'border-blue-500 bg-blue-500 text-white'
                          : 'border-gray-300 bg-white'
                      }`}>
                        {isSelected && <Check className="h-3 w-3" />}
                      </div>

                      {/* Info */}
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="font-medium text-gray-900 truncate">
                            {oficina.nomeFantasia}
                          </span>
                          <PlanoBadge plano={oficina.plano} />
                        </div>
                        <div className="mt-0.5 text-sm text-gray-500 truncate">
                          {oficina.cnpjCpf} | {oficina.email}
                        </div>
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 border-t px-6 py-3">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="rounded px-3 py-1 text-sm text-gray-600 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Anterior
              </button>
              <span className="text-sm text-gray-500">
                Página {page + 1} de {totalPages}
              </span>
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="rounded px-3 py-1 text-sm text-gray-600 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Próxima
              </button>
            </div>
          )}

          {/* Footer */}
          <div className="flex items-center justify-end gap-3 border-t px-6 py-4">
            <button
              onClick={onClose}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Cancelar
            </button>
            <button
              onClick={handleConfirm}
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              Confirmar ({selectedIds.size})
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OficinaSelectorModal;
