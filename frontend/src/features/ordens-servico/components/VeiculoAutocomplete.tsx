/**
 * Componente de autocomplete para seleção de veículos
 * Busca por placa e exibe placa, marca/modelo e cliente
 */

import { useState, useEffect, useRef } from 'react';
import { Search, X, Car } from 'lucide-react';
import { api } from '@/shared/services/api';

interface Cliente {
  id: string;
  nome: string;
  cpfCnpj: string;
}

interface Veiculo {
  id: string;
  placa: string;
  marca: string;
  modelo: string;
  ano: number;
  cor?: string;
  cliente: Cliente;
}

interface VeiculoAutocompleteProps {
  value: string;
  onChange: (veiculoId: string) => void;
  error?: string;
  required?: boolean;
}

export const VeiculoAutocomplete: React.FC<VeiculoAutocompleteProps> = ({
  value,
  onChange,
  error,
  required = false,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [veiculos, setVeiculos] = useState<Veiculo[]>([]);
  const [selectedVeiculo, setSelectedVeiculo] = useState<Veiculo | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  // Busca veículo selecionado ao carregar (para modo de edição)
  useEffect(() => {
    console.log('[VeiculoAutocomplete] Value recebido:', value);
    console.log('[VeiculoAutocomplete] selectedVeiculo atual:', selectedVeiculo);
    if (value && !selectedVeiculo) {
      console.log('[VeiculoAutocomplete] Buscando veículo por ID:', value);
      fetchVeiculoById(value);
    } else if (!value && selectedVeiculo) {
      // Se value foi limpo mas ainda há veículo selecionado, limpar
      console.log('[VeiculoAutocomplete] Value foi limpo, limpando selectedVeiculo');
      setSelectedVeiculo(null);
    }
  }, [value, selectedVeiculo]);

  // Fecha dropdown ao clicar fora
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Busca veículos com debounce
  useEffect(() => {
    if (!searchTerm || searchTerm.length < 2) {
      setVeiculos([]);
      return;
    }

    const delayDebounceFn = setTimeout(async () => {
      await searchVeiculos(searchTerm);
    }, 300);

    return () => clearTimeout(delayDebounceFn);
  }, [searchTerm]);

  const fetchVeiculoById = async (id: string) => {
    try {
      console.log('[VeiculoAutocomplete] Fazendo requisição para /veiculos/' + id);
      const { data } = await api.get<Veiculo>(`/veiculos/${id}`);
      console.log('[VeiculoAutocomplete] Veículo encontrado:', data);
      setSelectedVeiculo(data);
      console.log('[VeiculoAutocomplete] Veículo setado, NÃO chamando onChange (valor já está no form)');
    } catch (error) {
      console.error('[VeiculoAutocomplete] Erro ao buscar veículo:', error);
    }
  };

  const searchVeiculos = async (term: string) => {
    setIsLoading(true);
    try {
      const { data } = await api.get<{ content: Veiculo[] }>('/veiculos', {
        params: { placa: term, size: 10 },
      });
      setVeiculos(data.content);
      setIsOpen(true);
    } catch (error) {
      console.error('Erro ao buscar veículos:', error);
      setVeiculos([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSelect = (veiculo: Veiculo) => {
    setSelectedVeiculo(veiculo);
    onChange(veiculo.id);
    setSearchTerm('');
    setIsOpen(false);
  };

  const handleClear = () => {
    setSelectedVeiculo(null);
    onChange('');
    setSearchTerm('');
    setVeiculos([]);
  };

  const formatDisplay = (veiculo: Veiculo) => {
    return `${veiculo.placa} - ${veiculo.marca} ${veiculo.modelo} ${veiculo.ano}`;
  };

  return (
    <div ref={wrapperRef} className="relative">
      <label className="mb-1 block text-sm font-medium text-gray-700">
        Veículo {required && <span className="text-red-600">*</span>}
      </label>

      {/* Veículo selecionado */}
      {selectedVeiculo ? (
        <div className="flex items-center justify-between rounded-lg border border-gray-300 bg-gray-50 px-3 py-2">
          <div className="flex items-center gap-3">
            <Car className="h-5 w-5 text-gray-400" />
            <div>
              <p className="font-medium text-gray-900">{formatDisplay(selectedVeiculo)}</p>
              <p className="text-xs text-gray-500">
                Cliente: {selectedVeiculo.cliente.nome} - {selectedVeiculo.cliente.cpfCnpj}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleClear}
            className="rounded p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-600"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
      ) : (
        <>
          {/* Campo de busca */}
          <div className="relative">
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Buscar por placa (ex: ABC1234)"
              className={`w-full rounded-lg border px-3 py-2 pl-10 focus:outline-none focus:ring-2 ${
                error
                  ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                  : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500/20'
              }`}
            />
            <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
          </div>

          {/* Dropdown de resultados */}
          {isOpen && (
            <div className="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-lg border border-gray-300 bg-white shadow-lg">
              {isLoading ? (
                <div className="p-3 text-center text-sm text-gray-500">Buscando...</div>
              ) : veiculos.length === 0 ? (
                <div className="p-3 text-center text-sm text-gray-500">
                  {searchTerm.length < 2
                    ? 'Digite pelo menos 2 caracteres'
                    : 'Nenhum veículo encontrado'}
                </div>
              ) : (
                <ul>
                  {veiculos.map((veiculo) => (
                    <li key={veiculo.id}>
                      <button
                        type="button"
                        onClick={() => handleSelect(veiculo)}
                        className="w-full px-3 py-2 text-left hover:bg-blue-50"
                      >
                        <div className="flex items-center gap-2">
                          <Car className="h-4 w-4 text-gray-400" />
                          <div>
                            <p className="font-medium text-gray-900">{formatDisplay(veiculo)}</p>
                            <p className="text-xs text-gray-500">
                              {veiculo.cliente.nome} - {veiculo.cliente.cpfCnpj}
                            </p>
                          </div>
                        </div>
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </>
      )}

      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
    </div>
  );
};
