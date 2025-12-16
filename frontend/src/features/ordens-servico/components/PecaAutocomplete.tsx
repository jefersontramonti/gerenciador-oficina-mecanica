/**
 * Componente autocomplete para seleção de peça do estoque
 * Exibe código, descrição, quantidade disponível e valor de venda
 */

import { useState, useEffect, useRef } from 'react';
import { Package, Search, AlertTriangle } from 'lucide-react';
import { api } from '@/shared/services/api';
import type { Peca } from '@/features/estoque/types';

interface PecaAutocompleteProps {
  value: string;
  onChange: (pecaId: string, valorVenda: number, descricao: string) => void;
  error?: string;
  required?: boolean;
  disabled?: boolean;
}

export const PecaAutocomplete: React.FC<PecaAutocompleteProps> = ({
  value,
  onChange,
  error,
  required = false,
  disabled = false,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [pecas, setPecas] = useState<Peca[]>([]);
  const [selectedPeca, setSelectedPeca] = useState<Peca | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  // Buscar peças
  const fetchPecas = async (search: string) => {
    setIsLoading(true);
    try {
      const params = new URLSearchParams();

      // Buscar tanto por código quanto por descrição
      if (search) {
        // Tenta buscar por código primeiro
        params.append('codigo', search);
        params.append('descricao', search);
      }

      params.append('ativo', 'true');
      params.append('page', '0');
      params.append('size', '50'); // Aumentado para mostrar mais resultados
      params.append('sort', 'descricao,asc');

      const { data } = await api.get<any>('/estoque', { params });
      setPecas(data.content || []);
      setShowDropdown(true);
    } catch (error) {
      console.error('[PecaAutocomplete] Erro ao buscar peças:', error);
      setPecas([]);
    } finally {
      setIsLoading(false);
    }
  };

  // Buscar peça por ID quando value muda
  useEffect(() => {
    if (value && !selectedPeca) {
      const fetchPecaById = async () => {
        try {
          const { data } = await api.get<Peca>(`/estoque/${value}`);
          setSelectedPeca(data);
          setSearchTerm(`${data.codigo} - ${data.descricao}`);
        } catch (error) {
          console.error('[PecaAutocomplete] Erro ao buscar peça por ID:', error);
        }
      };
      fetchPecaById();
    }
  }, [value, selectedPeca]);

  // Fechar dropdown ao clicar fora
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setShowDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Debounce da busca
  useEffect(() => {
    const timer = setTimeout(() => {
      // Busca com qualquer termo (inclusive vazio para mostrar todas)
      fetchPecas(searchTerm);
    }, 300);

    return () => clearTimeout(timer);
  }, [searchTerm]);

  const handleSelect = (peca: Peca) => {
    setSelectedPeca(peca);
    setSearchTerm(`${peca.codigo} - ${peca.descricao}`);
    setShowDropdown(false);
    onChange(peca.id, peca.valorVenda, `${peca.codigo} - ${peca.descricao}`);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setSearchTerm(value);
    setSelectedPeca(null);

    if (!value) {
      onChange('', 0, '');
    }
  };

  return (
    <div ref={wrapperRef} className="relative">
      <label className="mb-1 block text-sm font-medium text-gray-700">
        <span className="flex items-center gap-2">
          <Package className="h-4 w-4" />
          Peça do Estoque {required && <span className="text-red-600">*</span>}
        </span>
      </label>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
        <input
          type="text"
          value={searchTerm}
          onChange={handleInputChange}
          onFocus={() => setShowDropdown(true)}
          disabled={disabled}
          className={`w-full rounded-lg border px-3 py-2 pl-10 focus:outline-none focus:ring-2 disabled:bg-gray-100 disabled:text-gray-500 ${
            error
              ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
              : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500/20'
          }`}
          placeholder="Digite código ou descrição da peça..."
        />

        {isLoading && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2">
            <div className="h-5 w-5 animate-spin rounded-full border-2 border-gray-300 border-t-blue-600"></div>
          </div>
        )}
      </div>

      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}

      {/* Dropdown com resultados */}
      {showDropdown && !disabled && pecas.length > 0 && (
        <div className="absolute z-50 mt-1 max-h-64 w-full overflow-auto rounded-lg border border-gray-200 bg-white shadow-lg">
          {pecas.map((peca) => (
            <button
              key={peca.id}
              type="button"
              onClick={() => handleSelect(peca)}
              className="w-full border-b border-gray-100 px-4 py-3 text-left hover:bg-blue-50 focus:bg-blue-50 focus:outline-none"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-gray-900">
                      {peca.codigo}
                    </span>
                    <span className="text-sm text-gray-600">
                      {peca.descricao}
                    </span>
                  </div>
                  <div className="mt-1 flex items-center gap-3 text-xs text-gray-500">
                    <span>Marca: {peca.marca || 'N/A'}</span>
                    <span className={`font-medium ${
                      peca.quantidadeAtual === 0
                        ? 'text-red-600'
                        : peca.estoqueBaixo
                        ? 'text-yellow-600'
                        : 'text-green-600'
                    }`}>
                      Estoque: {peca.quantidadeAtual} {peca.unidadeMedida}
                    </span>
                  </div>
                </div>
                <div className="flex flex-col items-end">
                  <span className="text-sm font-semibold text-green-600">
                    {new Intl.NumberFormat('pt-BR', {
                      style: 'currency',
                      currency: 'BRL',
                    }).format(peca.valorVenda)}
                  </span>
                  {peca.quantidadeAtual === 0 && (
                    <span className="mt-1 flex items-center gap-1 text-xs text-red-600">
                      <AlertTriangle className="h-3 w-3" />
                      Sem estoque
                    </span>
                  )}
                </div>
              </div>
            </button>
          ))}
        </div>
      )}

      {showDropdown && !disabled && !isLoading && pecas.length === 0 && (
        <div className="absolute z-50 mt-1 w-full rounded-lg border border-gray-200 bg-white p-4 text-center text-sm text-gray-500 shadow-lg">
          {searchTerm ? 'Nenhuma peça encontrada' : 'Digite para buscar peças'}
        </div>
      )}

      {/* Info da peça selecionada */}
      {selectedPeca && (
        <div className="mt-2 rounded-lg border border-green-200 bg-green-50 p-3">
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <p className="text-sm font-medium text-gray-900">
                {selectedPeca.codigo} - {selectedPeca.descricao}
              </p>
              <p className="mt-1 text-xs text-gray-600">
                Estoque: <span className="font-medium">{selectedPeca.quantidadeAtual} {selectedPeca.unidadeMedida}</span>
                {selectedPeca.marca && ` • Marca: ${selectedPeca.marca}`}
              </p>
            </div>
            <div className="text-right">
              <p className="text-sm font-semibold text-green-600">
                {new Intl.NumberFormat('pt-BR', {
                  style: 'currency',
                  currency: 'BRL',
                }).format(selectedPeca.valorVenda)}
              </p>
              <p className="text-xs text-gray-500">Valor de venda</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
